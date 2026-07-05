# kotoba-lang/identity

[![CI](https://github.com/kotoba-lang/identity/actions/workflows/ci.yml/badge.svg)](https://github.com/kotoba-lang/identity/actions/workflows/ci.yml)

Canonical identity record as data — a subject with one or more linked
identifiers (email, phone, `oauth-sub`+issuer, DID, ...) and verified
attributes. Every namespace is `.cljc`, zero third-party runtime deps. No
storage or crypto here — this is the pure record shape and merge logic;
[`kotoba-lang/identify`](https://github.com/kotoba-lang/identify) builds
identifier→identity lookup/resolution on top of it.

## Usage

```clojure
(require '[identity.core :as identity])

(def alice
  (-> (identity/new-identity {:id "u1" :created-at 1751500000000})
      (identity/add-identifier (identity/identifier :email "alice@example.com"))
      (identity/add-identifier (identity/identifier :oauth-sub "10298" "https://accounts.google.com"))
      (identity/set-attribute :display-name "Alice")))

(identity/find-identifier alice :oauth-sub "https://accounts.google.com")
;; => {:type :oauth-sub :value "10298" :issuer "https://accounts.google.com"}
```

`add-identifier` dedups by `[:type :issuer :value]`, and `merge-identities`
unions two identities' identifiers (dedup-safe) while merging attributes
(the second identity wins on conflicting keys) — useful when a login flow
discovers that two previously-separate identifiers belong to the same
person.

## Test

```bash
clojure -M:test
```
