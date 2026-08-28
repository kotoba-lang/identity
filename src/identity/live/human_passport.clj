(ns identity.live.human-passport
  "Read-only Optimism proof for the official Human Passport EAS schema."
  (:require [identity.adapters.eas :as eas]
            [identity.adapters.evm :as evm]
            [identity.adapters.human-passport :as passport]
            [identity.trust-policy :as trust-policy]
            [identity.trust-profile :as trust-profile]))

(def optimism
  {:namespace (:namespace trust-profile/human-passport-coordinate)
   :chain-id (:chainId trust-profile/human-passport-coordinate)
   :eas-address (:easAddress trust-profile/human-passport-coordinate)
   :schema-registry-address (:schemaRegistryAddress trust-profile/human-passport-coordinate)})

(def official-schema trust-profile/human-passport-schema-uid)
(def official-attester trust-profile/human-passport-attester)
(def sample-attestation
  "0xb6612e9191aaf5741420f4933a509c60f558b6fd2ee769befe3cc07805690a68")

(defn- flag-value [args flag]
  (some (fn [[a b]] (when (= a flag) b)) (partition 2 1 args)))

(defn -main [& args]
  (let [requested-uid (or (flag-value args "--uid")
                          (System/getenv "HUMAN_PASSPORT_ATTESTATION_UID"))
        attestation-uid (or requested-uid sample-attestation)
        reader (evm/eas-reader {:rpc-url (or (System/getenv "OPTIMISM_RPC_URL")
                                             "https://mainnet.optimism.io")})
        decoder (evm/human-passport-decoder)
        record (eas/read-attestation! reader optimism attestation-uid)
        schema (eas/read-schema! reader optimism (:schema-uid record))
        decoded (passport/decode-score! decoder (:schema schema) (:data record))
        now (quot (System/currentTimeMillis) 1000)
        policy trust-policy/itonami-human-passport-policy
        subject-id (str "did:pkh:eip155:10:" (.toLowerCase ^String (:recipient record)))
        lifecycle
        (try
          (passport/verify!
           reader decoder optimism attestation-uid
           {:eas {:allowed-schema-uids #{official-schema}
                  :allowed-attesters #{official-attester}
                  :now now}
            :scorer-id (:scorer-id policy)
            :minimum-score (:minimum-score policy)
            :policy-cid (:id policy)
            :issued-at (:issued-at policy)
            :subject-id subject-id})
          :accepted
          (catch clojure.lang.ExceptionInfo e
            (or (:identity.eas/problem (ex-data e))
                (:identity.human-passport/problem (ex-data e)))))]
    (when-not (= (if requested-uid :accepted :attestation/expired) lifecycle)
      (throw (ex-info (if requested-uid
                        "Requested Human Passport attestation did not pass"
                        "Official historical sample did not fail closed as expired")
                      {:attestation attestation-uid :lifecycle lifecycle})))
    (prn {:network "eip155:10"
          :attestation (:uid record)
          :recipient (:recipient record)
          :schema (:uid schema)
          :schema-current? (= passport/score-schema (:schema schema))
          :attester (:attester record)
          :scorer-id (:scorer-id decoded)
          :score (:score decoded)
          :threshold (:threshold decoded)
          :stamp-count (count (:stamps decoded))
          :lifecycle lifecycle
          :policy-id (:id policy)
          :write-performed? false})))
