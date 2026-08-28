# ADR-2608281000: External trust is scoped evidence, not ambient authority

**Status:** accepted — 2026-08-28

## Context

Kotoba has fail-closed adapters for Human Passport attestations on EAS and for
the three-registry ERC-8004 draft. Kotobase, Murakumo and Itonami also publish a
shared discovery profile. Adapter availability and discovery, however, do not
mean that a service has admitted evidence into an action decision. Treating the
catalog as authorization would turn a score or registry entry into ambient
authority and would obscure subject binding, freshness and revocation.

The three products need different evidence:

| Service role | Evidence use | Current enforcement |
| --- | --- | --- |
| Itonami human/organization operator | Human Passport as optional Sybil-resistance step-up | one evidence-only action |
| Kotobase evidence authority | EAS schema and attester provenance | active at authenticated `evidence.ingest`; evidence receipt only |
| Murakumo agent execution | ERC-8004 registration/reputation/validation | registry coordinate ungoverned, no live enforcement |

## Decision

The public contract is JSON Schema
`https://kotoba-lang.org/schemas/trust-profile/v1`. A profile separates
`sources` (what an adapter can verify) from `policy` (what this service actually
uses). Missing action policy, RPC coordinate, schema/attester allowlist,
subject binding, freshness, or lifecycle validity is a denial.

Human Passport support means the EAS Onchain Passport score schema only. It is
not KYC, personhood adjudication, universal reputation, or support for the
separate Sign Protocol Individual Verifications product. Itonami's first policy
is exactly `identity.sybil-step-up`: score at least 200000 from scorer 335,
maximum age 90 days, and the attestation recipient must equal the authenticated
Principal's verified EVM account. A successful check stores evidence and a
policy-bound decision receipt. It grants no capability and cannot authorize
money movement, tenant approval, agent delegation, or another action.

Kotobase admits an EAS claim only at the named evidence-ingest boundary with
a service-owned schema/attester allowlist. Its existing P4/P7 authorization
remains the capability decision; an attestation is input evidence only.

JavaScript edge hosts use `identity.adapters.evm-edge` for that boundary. The
edge reader checks `eth_chainId` before each `eth_call`, bounds JSON-RPC
responses, strictly decodes the EAS record and schema tuples, and only then
hands decoded values to `identity.adapters.eas/verify!`. Caller-supplied decoded
attestations are never accepted as chain evidence.

Murakumo will not activate ERC-8004 until governance selects a chain and the
Identity, Reputation and Validation registry coordinates and pins their code.
Registration, reputation and validation remain separate inputs. None alone
authorizes execution. Until then the public state is `supported-unbound`.

## Lifecycle and records

Verification performs external reads before the atomic ledger boundary. Every
accepted record carries the subject, source coordinate, attestation or agent
identifier, policy identifier, observation time and validity limit. Revoked,
expired, stale, future-dated, mismatched or unavailable evidence fails closed.
Refreshing evidence creates a new decision; it does not silently extend an old
one. Services must not cache acceptance beyond the claim validity limit.

The public profile is transport discovery, not a credential. HTTPS authenticates
delivery. A future signed projection may add content authentication without
changing the evidence or authorization model.

## Consequences

- The three public profiles can differ honestly while sharing one schema.
- A live verifier is measurable without widening production authority.
- ERC-8004 draft churn and chain deployment are governance changes, not hidden
  configuration changes.
- Further integrations require a named action, subject-binding rule, policy
  record, refusal tests, positive/expired/revoked proof, and an owner for
  refresh and telemetry.
