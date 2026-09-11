(ns identity.trust-policy-resource-test
  (:require [json.data-json :as json]
            [clojure.test :refer [deftest is]]
            [identity.trust-policy :as policy])
  (:import [java.math BigInteger]
           [java.nio.file Files Path]
           [java.security KeyFactory MessageDigest Signature]
           [java.security.spec X509EncodedKeySpec]
           [java.util Base64 HexFormat]))

(defn- sha256 [file]
  (format "%064x"
          (BigInteger. 1
                       (.digest (MessageDigest/getInstance "SHA-256")
                                (Files/readAllBytes (Path/of file
                                                            (make-array String 0)))))))

(deftest public-policy-id-is-the-exact-file-digest
  (let [file "resources/public/policies/trust/human-passport/itonami-v1.json"]
    (is (= (:id policy/itonami-human-passport-policy)
           (str "urn:sha256:" (sha256 file)))))
  (let [file "resources/public/policies/trust/eas/kotobase-v1.json"]
    (is (= (:id policy/kotobase-eas-policy)
           (str "urn:sha256:" (sha256 file)))))
  (let [file "resources/public/policies/trust/erc8004/murakumo-v1.json"]
    (is (= (:id policy/murakumo-erc8004-policy)
           (str "urn:sha256:" (sha256 file))))))

(def ^:private ed25519-spki-prefix "302a300506032b6570032100")

(defn- valid-ed25519-signature? [public-key-hex signature-base64 message]
  (let [spki (.parseHex (HexFormat/of) (str ed25519-spki-prefix public-key-hex))
        public-key (.generatePublic (KeyFactory/getInstance "Ed25519")
                                    (X509EncodedKeySpec. spki))
        verifier (Signature/getInstance "Ed25519")]
    (.initVerify verifier public-key)
    (.update verifier (.getBytes message "UTF-8"))
    (.verify verifier (.decode (Base64/getDecoder) signature-base64))))

(deftest murakumo-policy-has-a-valid-distinct-key-quorum
  (let [policy-file "resources/public/policies/trust/erc8004/murakumo-v1.json"
        envelope-file (str policy-file ".signature.json")
        envelope (json/read-str (slurp envelope-file) :key-fn keyword)
        policy-id (str "urn:sha256:" (sha256 policy-file))
        message (str "kotoba-trust-policy-v1\n" policy-id)
        signatures (:signatures envelope)
        valid (filter #(valid-ed25519-signature? (:publicKey %)
                                                 (:signature %)
                                                 message)
                      signatures)]
    (is (= 1 (:version envelope)))
    (is (= policy-id (:policyId envelope)))
    (is (= message (:signedMessage envelope)))
    (is (= "Ed25519" (:algorithm envelope)))
    (is (= 2 (:threshold envelope)))
    (is (= (count signatures) (count (distinct (map :keyId signatures)))))
    (is (<= (:threshold envelope) (count valid))
        "at least threshold distinct signatures must verify")))
