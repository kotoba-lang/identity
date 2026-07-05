(ns identity.core
  "Canonical identity record as data — a subject with one or more linked
  identifiers (email, phone, oauth sub+issuer, DID, ...) and verified
  attributes. No storage or crypto — this is the pure shape + merge logic;
  `kotoba-lang/identify` is the lookup/resolution layer built on top.")

(defn identifier
  "One identifier map: {:type type :value value}, plus :issuer when given.

    (identifier :email \"a@b.com\")
    (identifier :oauth-sub \"10298\" \"https://accounts.google.com\")"
  [type value & [issuer]]
  (cond-> {:type type :value value}
    issuer (assoc :issuer issuer)))

(defn new-identity
  "A fresh identity record. `id` is required and caller-supplied — this
  library never generates IDs. `identifiers` defaults to []."
  [{:keys [id identifiers attributes created-at]}]
  {:pre [(some? id)]}
  {:id id
   :identifiers (vec identifiers)
   :attributes (or attributes {})
   :created-at created-at})

(defn- identifier-key [{:keys [type issuer value]}]
  [type issuer value])

(defn add-identifier
  "Append `identifier` to `identity`, deduped by [:type :issuer :value] —
  adding an identical identifier twice is a no-op."
  [identity identifier]
  (let [existing (set (map identifier-key (:identifiers identity)))]
    (if (contains? existing (identifier-key identifier))
      identity
      (update identity :identifiers conj identifier))))

(defn find-identifier
  "First identifier matching `type` (and `issuer`, when given), or nil."
  [identity type & [issuer]]
  (some (fn [i]
          (when (and (= type (:type i))
                     (or (nil? issuer) (= issuer (:issuer i))))
            i))
        (:identifiers identity)))

(defn set-attribute [identity k v]
  (assoc-in identity [:attributes k] v))

(defn get-attribute [identity k]
  (get-in identity [:attributes k]))

(defn merge-identities
  "Merge `b` into `a`: keeps `a`'s :id/:created-at, identifiers are the
  union of both (deduped as in `add-identifier`), attributes are
  `(merge (:attributes a) (:attributes b))` — b wins on conflicting keys."
  [a b]
  (-> (reduce add-identifier a (:identifiers b))
      (update :attributes merge (:attributes b))))
