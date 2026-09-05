# ADR-001: Capability-Based Architecture

## Status

Accepted

## Context

RWA Shift Real Estate's off-chain backend spans distinct, independently-evolving domains:
organizations, assets, legal structures, offerings, investors, KYC, compliance, investments,
blockchain integration, ownership, distributions, documents, reporting, audit, and notifications.
A single undifferentiated layered application (controllers/services/repositories cutting across
all of these) would let any change touch unrelated domains and would give no structural signal
about which team or concern owns what.

## Decision

Organize the codebase around **capabilities** — cohesive business responsibilities, each realized
as one composite unit (see [ADR-002](ADR-002-composite-unit-structure.md)). A capability owns its
own data, its own state machines, and its own public contract (REST API + application-service
ports); it never reaches into another capability's persistence layer.

## Consequences

- New business capability → new unit, not a scattered change across existing controllers/services.
- Ownership boundaries are enforced structurally (package boundaries + ports), not just by
  convention or code review.
- Cross-capability data flow must be named and explicit (a `Port` interface), which is more
  verbose than direct repository access but makes every cross-capability dependency visible in one
  place — see [ADR-002](ADR-002-composite-unit-structure.md) for the mechanism.
- The trade-off accepted versus microservices is captured in [ADR-003](ADR-003-modular-monolith.md).
