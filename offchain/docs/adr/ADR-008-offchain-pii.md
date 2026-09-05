# ADR-008: Investor PII and Documents Stay Off-Chain

## Status

Accepted

## Context

Real-estate offerings require KYC/AML data, legal documents, and identity information about
investors. Ethereum (and any public/semi-public chain the platform might target) is a permanent,
publicly-readable ledger — anything written there is effectively disclosed forever, which is
incompatible with data-protection obligations around PII and with keeping sensitive commercial
documents (property valuations, legal agreements) confidential.

## Decision

Investor PII, KYC documents/decisions, property/legal-structure details, and offering commercial
terms are stored **only** in MySQL, never on-chain. The only investor-related data that crosses
onto the chain is the minimum ERC-3643 itself requires: a wallet address, an ISO-3166 numeric
country code, and an opaque OnchainID identity contract address — all relayed through
`RwaShiftIdentityGateway`, which by design never accepts a KYC document or PII payload (see
`onchain/README.md`). Distribution records anchor a `statementHash` (a hash of an off-chain
statement), never the statement's contents or bank/payment details.

## Consequences

- A full on-chain data leak (compromised RPC provider, chain reorg exposing mempool contents,
  public explorer indexing) cannot expose investor PII, KYC documents, or commercial deal terms —
  by construction, that data was never written there.
- Reconciling "why is this wallet eligible" requires joining off-chain KYC/compliance records with
  on-chain registration state — there is no single system with the full picture, which is the
  deliberate trade-off for keeping PII off a public ledger.
- Document storage (`document` unit) uses content-addressed SHA-256 hashing so document integrity
  can be verified without exposing document contents anywhere but the storage backend itself.
