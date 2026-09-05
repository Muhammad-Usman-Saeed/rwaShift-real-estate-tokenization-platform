# ADR-003: Modular Monolith, Not Microservices

## Status

Accepted

## Context

The capability partition ([ADR-001](ADR-001-capability-based-architecture.md)) is the kind of
seam that often gets mapped 1:1 onto microservices. RWA Shift Real Estate V1 has one team, one
deployable's worth of operational budget, and domains (offering ↔ investment ↔ tokenization ↔
ownership) with tight read/write consistency needs — e.g. an investment's eligibility check reads
KYC + compliance + offering state synchronously in one request.

## Decision

Ship as **one Spring Boot deployable, one MySQL database**, internally partitioned into the 16
capability units from ADR-001/ADR-002. No network hop, no distributed transaction, no service
mesh between units — cross-unit calls are plain Java method calls through ports, in the same
JVM, in the same database transaction where needed.

## Consequences

- Cross-unit invariants (e.g. "an investment can't be confirmed unless compliance says eligible")
  are enforced with ordinary `@Transactional` boundaries, not sagas or eventual consistency.
- Single deployment pipeline, single set of infrastructure to operate (see `docker-compose.yml`)
  — appropriate for the team size and stage this is built for.
- The explicit port-based unit boundaries from ADR-002 mean that if a specific capability (most
  plausibly Tokenization, given its different scaling/latency profile) ever needs to be extracted
  into its own service, the seam already exists: replace the in-process port implementation with
  an HTTP/message-based adapter behind the same interface. This ADR does not commit to ever doing
  that extraction — it only notes the migration path is cheap because of ADR-002's discipline.
- Accepted trade-off: all 16 units share fate on deploys, scaling, and uptime. Judged acceptable
  for V1's scale and team size.
