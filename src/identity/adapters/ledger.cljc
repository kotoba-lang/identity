(ns identity.adapters.ledger
  (:require [identity.causal :as causal]
            [identity.datom :as d]
            [identity.validate :as v]))

(defprotocol ILedger
  (transact! [ledger datoms opts]))

(defn persist-subject! [ledger subject opts]
  (v/valid! subject)
  (transact! ledger (d/subject-datoms subject) opts))

(defn persist-evidence! [ledger evidence opts]
  (v/valid! evidence)
  (transact! ledger (d/evidence-datoms evidence) opts))

(defn persist-attestation! [ledger attestation opts]
  (v/valid! attestation)
  (transact! ledger (d/attestation-datoms attestation) opts))

(defn persist-causal! [ledger record opts]
  (causal/valid! record)
  (transact! ledger (d/causal-datoms record) opts))

(defn persist-transition!
  "Persist the transition and new epoch atomically after basis validation."
  [ledger transition new-epoch basis opts]
  (let [{:identity.causal/keys [transition new-epoch]}
        (causal/validate-transition! transition new-epoch basis)]
    (transact! ledger
               (into (d/causal-datoms transition)
                     (d/causal-datoms new-epoch))
               opts)))
