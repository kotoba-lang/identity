# identity

Portable EDN model for identity subjects, attestations, and evidence references.

This repo is the shared substrate for identity-facing kotoba-lang libraries. It
does not verify documents, run biometrics, or score compliance risk.

## External trust adapters

`identity.adapters.eas` verifies host-decoded Ethereum Attestation Service
records against an explicit chain/contract coordinate plus mandatory schema and
attester allowlists. Revoked, expired, future-dated, mismatched, or unanchored
records fail closed.

`identity.adapters.human-passport` composes that EAS boundary with Human
Passport's current score schema. It pins the scorer, independently reapplies a
deployment minimum, checks the attested passing flag, and expires otherwise
non-expiring score attestations after 90 days. The result is a scoped
`[:identity :sybil-resistance]` claim and Sekisho evidence—not a general Kotoba
trust score and not KYC.

`identity.adapters.erc8004` models the current draft's separate Identity,
Reputation, and Validation registries. Registration creates an `:agent`
subject. Reputation and validation create scoped trust claims only from
explicitly allowlisted clients/validators; low results remain structured
refusals. The historical Etzhayyim monolithic `ERC-8004-shaped` contract is not
treated as compatible with this adapter.

All three namespaces are pure verification/composition boundaries. Production
hosts provide pinned RPC, ABI decoding, registration-document retrieval, and
transport. `identity.adapters.ledger/persist-trust-bundle!` atomically commits a
fully verified subject, evidence, attestations, and scoped claims.

`identity.directory` defines the portable organization directory used by
cloud-itonami: domain-scoped users and groups, lifecycle status, delegated
administrator roles, and active-seat counting.
