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
  (is (= 7776000 (get-in example [:sources :humanPassport :maximumAgeSeconds]))))

(deftest erc8004-never-invents-a-live-coordinate
  (testing "an implemented draft reader is distinct from a governed deployment"
    (is (= "supported-unbound" (get-in example [:sources :erc8004 :status])))
    (is (nil? (get-in example [:sources :erc8004 :registryBinding])))
    (is (false? (get-in example [:claims :erc8004LiveRegistry])))))
