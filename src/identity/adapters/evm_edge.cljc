(ns identity.adapters.evm-edge
  "Pinned, read-only EAS transport for JavaScript edge hosts.

  The portable EAS policy remains in identity.adapters.eas. This namespace
  only supplies bounded JSON-RPC transport and strict ABI decoding for hosts
  such as Cloudflare Workers."
  (:require [clojure.string :as str]
            [identity.adapters.eas :as eas])
  #?(:clj (:import (java.math BigInteger)
                   (java.nio.charset StandardCharsets))))

(def ^:private get-attestation-selector "a3112a64")
(def ^:private get-schema-selector "a2ea7c6e")
(def ^:private max-rpc-text-chars (* 4 1024 1024))

(defn- fail! [code details]
  (throw (ex-info "EVM edge trust reader rejected input"
                  (assoc details :identity.evm-edge/problem code))))

(defn- strip-0x [x]
  (if (and (string? x) (str/starts-with? x "0x")) (subs x 2) x))

(defn- hex-string? [x]
  (boolean (and (string? x)
                (even? (count (strip-0x x)))
                (re-matches #"[0-9a-fA-F]*" (strip-0x x)))))

(defn- bytes-length [hex]
  (quot (count (strip-0x hex)) 2))

(defn- ensure-range! [hex offset length label]
  (when-not (and (hex-string? hex)
                 (integer? offset) (not (neg? offset))
                 (integer? length) (not (neg? length))
                 (<= (+ offset length) (bytes-length hex)))
    (fail! :abi/out-of-bounds
           {:field label :offset offset :length length
            :available (when (hex-string? hex) (bytes-length hex))})))

(defn- slice-bytes [hex offset length label]
  (ensure-range! hex offset length label)
  (subs (strip-0x hex) (* 2 offset) (* 2 (+ offset length))))

(defn- safe-uint [raw label]
  #?(:clj
     (try (.longValueExact (BigInteger. raw 16))
          (catch ArithmeticException _
            (fail! :abi/integer-overflow {:field label})))
     :cljs
     (let [n (js/BigInt (str "0x" raw))
           maximum (js/BigInt js/Number.MAX_SAFE_INTEGER)]
       (when (> n maximum)
         (fail! :abi/integer-overflow {:field label}))
       (js/Number n))))

(defn- uint-at [hex offset label]
  (safe-uint (slice-bytes hex offset 32 label) label))

(defn- offset-at [hex offset label]
  (uint-at hex offset label))

(defn- bytes32-at [hex offset label]
  (str "0x" (str/lower-case (slice-bytes hex offset 32 label))))

(defn- address-at [hex offset label]
  (let [word (slice-bytes hex offset 32 label)]
    (when-not (re-matches #"0{24}[0-9a-fA-F]{40}" word)
      (fail! :abi/address-padding {:field label}))
    (str "0x" (str/lower-case (subs word 24)))))

(defn- bool-at [hex offset label]
  (case (uint-at hex offset label)
    0 false
    1 true
    (fail! :abi/bool {:field label})))

(defn- bytes-at [hex offset label]
  (let [length (uint-at hex offset (str label ".length"))]
    (slice-bytes hex (+ offset 32) length label)))

(defn- utf8-at [hex offset label]
  (let [raw (bytes-at hex offset label)]
    #?(:clj
       (String. (byte-array
                 (map #(unchecked-byte (Integer/parseInt (apply str %) 16))
                      (partition 2 raw)))
                StandardCharsets/UTF_8)
       :cljs
       (let [bytes (js/Uint8Array.
                    (clj->js (mapv #(js/parseInt (apply str %) 16)
                                   (partition 2 raw))))]
         (.decode (js/TextDecoder. "utf-8" #js {:fatal true}) bytes)))))

(defn decode-eas-attestation-result
  "Decode EAS getAttestation(bytes32), rejecting malformed offsets and words."
  [hex]
  (let [base (offset-at hex 0 "attestation.tuple")
        data-offset (offset-at hex (+ base (* 9 32)) "attestation.data-offset")]
    {:uid (bytes32-at hex base "attestation.uid")
     :schema-uid (bytes32-at hex (+ base 32) "attestation.schema")
     :time (uint-at hex (+ base (* 2 32)) "attestation.time")
     :expiration-time (uint-at hex (+ base (* 3 32)) "attestation.expiration-time")
     :revocation-time (uint-at hex (+ base (* 4 32)) "attestation.revocation-time")
     :reference-uid (bytes32-at hex (+ base (* 5 32)) "attestation.ref-uid")
     :recipient (address-at hex (+ base (* 6 32)) "attestation.recipient")
     :attester (address-at hex (+ base (* 7 32)) "attestation.attester")
     :revocable? (bool-at hex (+ base (* 8 32)) "attestation.revocable")
     :revoked? (pos? (uint-at hex (+ base (* 4 32)) "attestation.revocation-time"))
     :data (str "0x" (bytes-at hex (+ base data-offset) "attestation.data"))}))

(defn decode-eas-schema-result
  "Decode EAS SchemaRegistry getSchema(bytes32)."
  [hex]
  (let [base (offset-at hex 0 "schema.tuple")
        schema-offset (offset-at hex (+ base (* 3 32)) "schema.schema-offset")]
    {:uid (bytes32-at hex base "schema.uid")
     :resolver (address-at hex (+ base 32) "schema.resolver")
     :revocable? (bool-at hex (+ base (* 2 32)) "schema.revocable")
     :schema (utf8-at hex (+ base schema-offset) "schema.schema")}))

#?(:cljs
   (do
     (defn- rpc-call! [fetch-fn rpc-url method params]
       (let [id (str "kotoba-trust-" (js/crypto.randomUUID))]
         (-> (fetch-fn rpc-url
                       #js {:method "POST"
                            :headers #js {"content-type" "application/json"}
                            :body (js/JSON.stringify
                                   (clj->js {:jsonrpc "2.0" :id id
                                             :method method :params params}))})
             (.then (fn [response]
                      (when-not (.-ok response)
                        (fail! :rpc/http {:method method :status (.-status response)}))
                      (.text response)))
             (.then (fn [text]
                      (when (> (count text) max-rpc-text-chars)
                        (fail! :rpc/response-too-large {:method method}))
                      (let [body (try (js/JSON.parse text)
                                      (catch :default _
                                        (fail! :rpc/json {:method method})))]
                        (when-not (= id (aget body "id"))
                          (fail! :rpc/id {:method method}))
                        (when (aget body "error")
                          (fail! :rpc/error {:method method}))
                        (let [result (aget body "result")]
                          (when-not (string? result)
                            (fail! :rpc/no-result {:method method}))
                          result)))))))

     (defn- assert-chain! [fetch-fn rpc-url expected-chain-id]
       (-> (rpc-call! fetch-fn rpc-url "eth_chainId" [])
           (.then (fn [result]
                    (when-not (re-matches #"0x[0-9a-fA-F]+" result)
                      (fail! :rpc/chain-id-result {:result result}))
                    (let [actual (js/parseInt (subs result 2) 16)]
                      (when-not (= expected-chain-id actual)
                        (fail! :rpc/chain-mismatch
                               {:expected expected-chain-id :actual actual})))))))

     (defn- eth-call! [fetch-fn rpc-url coordinate to data]
       (-> (assert-chain! fetch-fn rpc-url (:chain-id coordinate))
           (.then (fn [_]
                    (rpc-call! fetch-fn rpc-url "eth_call"
                               [{:to to :data data} "latest"])))))

     (defn verify-eas!
       "Read one EAS record and its schema from the pinned chain, then apply
       identity.adapters.eas policy. Returns Promise<verified evidence>."
       [{:keys [fetch-fn rpc-url coordinate attestation-uid
                allowed-schema-uids allowed-attesters now]}]
       (when-not (and (string? rpc-url) (re-matches #"https://\S+" rpc-url))
         (fail! :rpc/url {:rpc-url rpc-url}))
       (when-not (and (string? attestation-uid)
                      (re-matches #"0x[0-9a-fA-F]{64}" attestation-uid))
         (fail! :attestation/uid {:uid attestation-uid}))
       (let [fetch-fn (or fetch-fn js/fetch)
             call-data (str "0x" get-attestation-selector (subs attestation-uid 2))]
         (-> (eth-call! fetch-fn rpc-url coordinate (:eas-address coordinate) call-data)
             (.then (fn [encoded]
                      (let [record (decode-eas-attestation-result encoded)
                            schema-call (str "0x" get-schema-selector
                                             (subs (:schema-uid record) 2))]
                        (-> (eth-call! fetch-fn rpc-url coordinate
                                       (:schema-registry-address coordinate) schema-call)
                            (.then
                             (fn [schema-encoded]
                               (eas/verify!
                                (eas/static-reader
                                 {:attestations {attestation-uid record}
                                  :schemas {(:schema-uid record)
                                            (decode-eas-schema-result schema-encoded)}})
                                coordinate attestation-uid
                                {:allowed-schema-uids allowed-schema-uids
                                 :allowed-attesters allowed-attesters
                                 :now now}))))))))))))
