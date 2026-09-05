# ADR-010: Explicit Blockchain Transaction Lifecycle State Machine

## Status

Accepted

## Context

Every on-chain write (deploying an offering's token suite, registering an investor identity,
minting units, recording a distribution) is asynchronous and can fail at multiple points: before
submission (RPC error), after submission but before mining (dropped/replaced/reorged), or after
mining (reverted). Callers (Investment, Offering) need to know definitively when their requested
on-chain effect has actually happened, without polling raw RPC calls themselves or blocking a
request thread on transaction confirmation.

## Decision

Model every on-chain write as a `BlockchainTransaction` with an explicit state machine:

```
CREATED → SUBMITTED → PENDING → CONFIRMED
                            └──→ FAILED
```

`BlockchainTransactionManager.createAndSubmit(...)` creates the row and submits the raw
transaction (`Web3jContractGateway`, `RawTransactionManager`, not generated contract wrappers). A
`@Scheduled pollOutstandingTransactions()` job polls receipts for every `SUBMITTED`/`PENDING`
transaction and transitions to `CONFIRMED` or `FAILED` once a receipt is available (respecting
`rwashift.blockchain.confirmations-required`). On a terminal transition, the manager calls back
into the originating unit through `TokenIssuanceCallbackPort` (Investment) or
`OfferingTokenizationCallbackPort` (Offering) — both injected `@Lazy` to break the circular
dependency that would otherwise exist (Investment/Offering depend on Tokenization to *request* a
transaction; Tokenization depends back on them to *report* its outcome).

Offering token deployment is two-phase and modeled explicitly: `createOffering` confirming
auto-submits a follow-up `unpause` transaction (T-REX tokens deploy paused); only once *that*
transaction also confirms does the manager invoke `OfferingTokenizationCallbackPort.onTokenizationConfirmed`.

## Consequences

- No unit blocks a request thread waiting for chain confirmation — `createAndSubmit` returns
  immediately with a `CREATED`/`SUBMITTED` transaction; the caller's own domain entity
  (`Investment`, `Offering`) sits in its own "pending on-chain effect" state until the callback
  fires.
- A crashed/restarted app resumes correctly: outstanding transactions are rows in MySQL, not
  in-memory state, so `pollOutstandingTransactions()` picks them up again on the next tick.
- The two-phase offering-deployment sequencing is explicit in code (not implied by ordering of
  unrelated calls), so "token deployed but still paused" is a representable, queryable state
  rather than a race condition.
- Idempotency of the callback itself is the callee's responsibility (Investment/Offering's own
  state-machine `canTransitionTo` checks reject a repeated callback for an already-settled entity).
