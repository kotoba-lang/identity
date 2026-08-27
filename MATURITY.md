# Maturity

**Level: R2 live adapter**

Implemented:
- Subject, evidence reference, and attestation models.
- Validation for known subject and evidence kinds, required IDs, attestation fields, and non-adjudicating records.
- Datom emitters for subjects, evidence references, and attestations.
- Ledger adapter boundary for validated subject/evidence/attestation datom transactions.
- Durable EDN ledger implementation.
- Datomic/Kotoba ledger backend adapter with transaction option propagation.
- DID resolver adapter boundary and DID document to subject binding.
- Verifiable Credential verifier boundary with evidence and attestation binding.
- Cross-source subject merge policy with aliases and conflict markers.
- Contract tests for subject shape, invalid records, attestation datoms, DID resolution, VC binding, merge policy, ledger payloads, and durable transaction reload.
- Causal identity epochs, attributed identifier-link claims, scoped evaluator
  trust claims, inheritable obligations, and basis-bound epoch transitions.
- Atomic transition persistence: the successor epoch starts at trust zero only
  when every open obligation is carried and every prior active grant is named
  for revocation.
- Fail-closed EAS verification with explicit chain/contract coordinates,
  schema and attester allowlists, revocation, expiration, and time checks.
- Human Passport current-schema normalization into a 90-day scoped
  Sybil-resistance claim and Sekisho evidence, without importing its score as a
  general trust score.
- Current draft ERC-8004 three-registry registration, reputation, and
  validation normalization with mandatory client/validator allowlists.
- Atomic persistence of externally verified trust bundles.

Not yet R2:
- Live RPC/ABI/document-fetch implementations for the new external adapters.
  Those remain host responsibilities; this repo currently proves the portable
  reader/decoder contracts and fail-closed policy behavior.
- Current live state of the historical Etzhayyim private chain is unverified;
  its old monolithic contract is explicitly outside current ERC-8004 support.
