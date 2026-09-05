# ADR-007: The Chain Is the Source of Truth for Ownership; MySQL Owns Everything Else

## Status

Accepted

## Context

Token balances exist in two places once tokenization is live: on-chain (the actual ERC-3643
`Token` contract's balances, enforced by the chain) and, potentially, off-chain (if any unit were
tempted to keep its own "current balance" column for convenience/query performance). Two
authoritative copies of the same fact is a well-known source of drift bugs — a failed or reorged
transaction, a missed event, or a race between "off-chain write" and "on-chain confirmation" would
silently desynchronize them.

## Decision

- **The chain is the sole source of truth for token ownership.** No unit other than `tokenization`
  and `ownership` is allowed to reason about balances directly.
- `ownership.OwnershipRecord` is an explicit, documented **read-model projection**, rebuilt purely
  from indexed `Transfer` events (`BlockchainEventIndexer`, idempotent on `(txHash, logIndex)`).
  It is written only by the indexer, via `OwnershipProjectionPort`, and is **never** used to
  authorize a transfer or issuance decision — those decisions are made on-chain by
  `IdentityRegistry.isVerified` and enforced by the `Token` contract itself.
- Every other capability (organizations, assets, legal structures, offering metadata, investor
  profile/PII, KYC decisions, compliance eligibility *decisions* [not enforcement], investments,
  documents, audit) is authoritative in MySQL and never touches the chain.
- **Tokenization is the only unit permitted to depend on Web3j** or hold contract
  addresses/ABIs. Every other unit that needs an on-chain effect (issuing units, registering an
  identity, recording a distribution) goes through one of Tokenization's ports
  (`TokenIssuancePort`, `IdentityRegistrationPort`, `DistributionRecordingPort`) — never directly.

## Consequences

- If the Ownership projection and the chain ever disagree (a missed block range, a bug in the
  indexer), the fix is always "re-index from the chain," never "trust the projection and patch the
  chain" — the projection has no independent authority to protect.
- Reporting and UI reads of "who owns how many units" go through `OwnershipLookupPort`
  (fast, MySQL-backed) rather than an `eth_call` per request, at the accepted cost of eventual
  consistency bounded by the indexer's poll interval (`BLOCKCHAIN_POLL_INTERVAL_MS`, default 4s).
- A security/compliance review of "does any off-chain code path let someone move tokens without
  the chain's involvement" reduces to "does anything besides `tokenization` call Web3j" — grep-able
  because of the single-unit constraint.
