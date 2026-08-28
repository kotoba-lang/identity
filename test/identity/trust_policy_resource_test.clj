(ns identity.trust-policy-resource-test
  (:require [clojure.test :refer [deftest is]]
            [identity.trust-policy :as policy])
  (:import [java.math BigInteger]
           [java.nio.file Files Path]
           [java.security MessageDigest]))

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
           (str "urn:sha256:" (sha256 file))))))
