(ns identity.trust-profile-test
  (:require [clojure.test :refer [deftest is testing]]
            [identity.trust-profile :as trust-profile]))

(def example
  (trust-profile/profile
   {:origin "https://example.test"
    :authorityDid "did:web:example.test"
    :role "evidence-authority"
    :identityEndpoint "https://example.test/api/identity"}))

(deftest publishes-one-scoped-fail-closed-contract
  (is (= trust-profile/schema (:schema example)))
  (is (= "https://example.test" (get-in example [:service :origin])))
  (is (= "identity/sybil-resistance"
         (get-in example [:sources :humanPassport :purpose])))
  (is (false? (get-in example [:semantics :universalTrustScore])))
  (is (true? (get-in example [:semantics :failClosed])))
  (is (= 7776000 (get-in example [:sources :humanPassport :maximumAgeSeconds])))
  (is (= "adapter-available-not-enforced"
         (get-in example [:policy :ethereumAttestationService :status]))))

(deftest service-role-selects-use-policy-not-a-universal-policy
  (let [itonami (trust-profile/profile
                 {:origin "https://itonami.cloud"
                  :authorityDid "did:web:itonami.cloud"
                  :role "human-organization-operator"
                  :identityEndpoint "https://itonami.cloud/.well-known/did.json"})]
    (is (= 200000 (get-in itonami [:policy :humanPassport :minimumScore])))
    (is (= "identity.sybil-step-up"
           (get-in itonami [:policy :humanPassport :action])))
    (is (false? (get-in itonami [:policy :humanPassport :grantsCapability])))))

(deftest erc8004-never-invents-a-live-coordinate
  (testing "an implemented draft reader is distinct from a governed deployment"
    (is (= "supported-unbound" (get-in example [:sources :erc8004 :status])))
    (is (nil? (get-in example [:sources :erc8004 :registryBinding])))
    (is (false? (get-in example [:claims :erc8004LiveRegistry])))))
