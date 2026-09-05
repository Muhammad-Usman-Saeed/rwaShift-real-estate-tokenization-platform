# ADR-006: Liquibase for Schema Management, Never `ddl-auto=update`

## Status

Accepted

## Context

Sixteen capability units, each owning tables, need a schema-evolution mechanism that is
reviewable, reproducible across environments (local, CI/Testcontainers, and eventually staging/
production), and never silently diverges from what the entities expect at runtime. Flyway was
considered and rejected per the team's mandate; Hibernate's `ddl-auto=update`/`create` was
rejected as unsafe for anything beyond prototyping.

## Decision

**Liquibase YAML changelogs**, one folder per capability unit under
`src/main/resources/db/changelog/`, included in dependency order (organization → iam → asset →
legalstructure → offering → investor → kyc → compliance → investment → tokenization → ownership →
distribution → document → audit) by a single `db.changelog-master.yaml`. Hibernate's own
`ddl-auto` is fixed to `validate` — it never creates or alters schema, only confirms the entity
mapping matches what Liquibase already applied.

## Consequences

- Every schema change is an explicit, versioned, auditable changelog file — never an implicit
  side effect of starting the app with a new entity field.
- A mismatch between a JPA entity and the actual schema (e.g. a Liquibase changelog using
  `CHAR(36)` where the entity expects a `String` Hibernate maps to `VARCHAR`) fails app startup
  immediately via Hibernate's `validate` check, rather than succeeding with a subtly wrong column
  type that only breaks at write time.
- Changelog dependency order matters and is maintained by hand in the master changelog — a unit's
  changelog must come after every unit whose tables it foreign-keys.
- Testcontainers-backed integration tests (`AbstractIntegrationTest`) run every changelog against
  a fresh MySQL 8.4 container per test JVM, so changelog correctness is verified on every test run,
  not just in a separate migration-testing pipeline.
