# identity

Portable EDN model for identity subjects, attestations, and evidence references.

This repo is the shared substrate for identity-facing kotoba-lang libraries. It
does not verify documents, run biometrics, or score compliance risk.

`identity.directory` defines the portable organization directory used by
cloud-itonami: domain-scoped users and groups, lifecycle status, delegated
administrator roles, and active-seat counting.
