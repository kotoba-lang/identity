(ns identity.core-test
  (:require [clojure.test :refer [deftest testing is]]
            [identity.core :as identity]))

(deftest new-identity-test
  (testing "required id, defaults for the rest"
    (let [i (identity/new-identity {:id "u1"})]
      (is (= "u1" (:id i)))
      (is (= [] (:identifiers i)))
      (is (= {} (:attributes i)))))
  (testing "carries through identifiers/attributes/created-at"
    (let [ident (identity/identifier :email "a@b.com")
          i (identity/new-identity {:id "u1" :identifiers [ident]
                                     :attributes {:name "A"} :created-at 100})]
      (is (= [ident] (:identifiers i)))
      (is (= {:name "A"} (:attributes i)))
      (is (= 100 (:created-at i))))))

(deftest identifier-test
  (is (= {:type :email :value "a@b.com"} (identity/identifier :email "a@b.com")))
  (is (= {:type :oauth-sub :value "10298" :issuer "https://accounts.google.com"}
         (identity/identifier :oauth-sub "10298" "https://accounts.google.com"))))

(deftest add-identifier-test
  (let [i (identity/new-identity {:id "u1"})
        e (identity/identifier :email "a@b.com")]
    (testing "appends"
      (is (= [e] (:identifiers (identity/add-identifier i e)))))
    (testing "dedup by [:type :issuer :value] — identical add is a no-op"
      (let [twice (-> i (identity/add-identifier e) (identity/add-identifier e))]
        (is (= [e] (:identifiers twice)))))
    (testing "same type+value but different issuer is NOT a dup"
      (let [g (identity/identifier :oauth-sub "1" "https://a.example")
            m (identity/identifier :oauth-sub "1" "https://b.example")
            both (-> i (identity/add-identifier g) (identity/add-identifier m))]
        (is (= 2 (count (:identifiers both))))))))

(deftest find-identifier-test
  (let [e (identity/identifier :email "a@b.com")
        g (identity/identifier :oauth-sub "1" "https://accounts.google.com")
        i (-> (identity/new-identity {:id "u1"})
              (identity/add-identifier e)
              (identity/add-identifier g))]
    (testing "matches on type alone when issuer omitted"
      (is (= e (identity/find-identifier i :email))))
    (testing "matches on type+issuer when issuer given"
      (is (= g (identity/find-identifier i :oauth-sub "https://accounts.google.com"))))
    (testing "no match when issuer given but wrong"
      (is (nil? (identity/find-identifier i :oauth-sub "https://wrong.example"))))
    (testing "no match for absent type"
      (is (nil? (identity/find-identifier i :phone))))))

(deftest attribute-test
  (let [i (identity/new-identity {:id "u1"})]
    (is (= "Ada" (-> i (identity/set-attribute :name "Ada") (identity/get-attribute :name))))
    (is (nil? (identity/get-attribute i :name)))))

(deftest merge-identities-test
  (let [ea (identity/identifier :email "a@b.com")
        eb (identity/identifier :phone "+15551234567")
        a (-> (identity/new-identity {:id "u1" :created-at 1 :attributes {:name "A" :locale "en"}})
              (identity/add-identifier ea))
        b (-> (identity/new-identity {:id "u2" :created-at 2 :attributes {:locale "ja" :verified true}})
              (identity/add-identifier eb))
        m (identity/merge-identities a b)]
    (testing "keeps a's id and created-at"
      (is (= "u1" (:id m)))
      (is (= 1 (:created-at m))))
    (testing "identifiers are the union"
      (is (= #{ea eb} (set (:identifiers m)))))
    (testing "attributes merged, b wins on conflict"
      (is (= {:name "A" :locale "ja" :verified true} (:attributes m))))
    (testing "merging is dedup-safe (re-merging a with itself changes nothing)"
      (is (= (:identifiers a) (:identifiers (identity/merge-identities a a)))))))
