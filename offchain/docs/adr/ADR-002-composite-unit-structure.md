# ADR-002: Composite Unit Internal Structure

## Status

Accepted

## Context

Given capability units as the top-level partition ([ADR-001](ADR-001-capability-based-architecture.md)),
each unit still needs an internal structure that (a) keeps domain logic free of framework/persistence
concerns, (b) makes the unit's public contract to other units explicit and narrow, and (c) is
consistent enough across all 16 units that any engineer can navigate any unit the same way.

## Decision

Every unit follows the same hexagonal/DDD-flavored package layout:

```
units/<capability>/
├── api/
│   ├── rest/        # @RestController — HTTP adapter
│   └── dto/         # request/response DTOs, never domain entities
├── application/
│   ├── command/      # command records (write intents)
│   ├── query/         # query records (read intents), where useful
│   ├── service/       # @Service application services — orchestration, transaction boundary
│   └── port/           # interfaces this unit exposes to (or depends on from) other units
├── domain/
│   ├── model/         # entities, value objects, state-machine enums
│   ├── event/          # domain events (where used)
│   ├── policy/          # pluggable business rules (e.g. compliance's EligibilityRule)
│   └── repository/      # Spring Data JPA repository interfaces
└── infrastructure/
    ├── persistence/     # repository adapters, if any indirection beyond Spring Data is needed
    ├── integration/      # adapters implementing another unit's port (e.g. Web3jContractGateway)
    └── configuration/     # unit-local @Configuration classes
```

**Cross-unit communication rule**: a unit may depend on another unit's `application.port`
interface and the DTOs/snapshot records it declares (e.g. `OfferingSnapshot`,
`InvestorSnapshot`). It may never `@Autowired` another unit's JPA repository, entity, or
application service directly. The port is implemented by the *owning* unit's application
service (e.g. `OfferingApplicationService implements OfferingLookupPort,
OfferingTokenizationCallbackPort`), keeping the implementation colocated with the domain it
serves while the interface lives wherever is most convenient for the consumer to depend on.

## Consequences

- Every unit is navigable identically — reduces onboarding cost and code-review friction.
- The full list of a unit's external dependencies is exactly its `application/port` imports from
  other units — greppable, and a natural place to enforce with an architecture test if the
  codebase grows past what code review alone can catch.
- Slightly more ceremony for simple pass-through data (a Port + a Snapshot record where a shared
  entity reference "would be simpler") — accepted deliberately, since it's what prevents a
  modular monolith from silently regressing into a distributed monolith of tangled entity
  references.
