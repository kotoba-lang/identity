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
  (is (= "active"
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

(deftest evidence-authority-publishes-the-bounded-eas-action
  (is (= "evidence.ingest"
         (get-in example [:policy :ethereumAttestationService :action])))
  (is (= "urn:sha256:69f422026ab1efb38c7848a1e1bc5a0b2c52a4de6ecd774eef7b96cc0af6a6c1"
         (get-in example [:policy :ethereumAttestationService :id])))
  (is (= "/xrpc/ai.gftd.apps.kotobase.evidence.ingest"
         (get-in example [:policy :ethereumAttestationService :verificationEndpoint])))
  (is (= "evidence-only"
         (get-in example [:policy :ethereumAttestationService :effect])))
  (is (false? (get-in example [:policy :ethereumAttestationService :grantsCapability]))))
