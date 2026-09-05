# ADR-005: MySQL 8, Never PostgreSQL

## Status

Accepted

## Context

The platform needs a single relational store for all off-chain state (organizations, assets,
offerings, investors, KYC, investments, blockchain-transaction bookkeeping, ownership projection,
distributions, documents, audit). The team's mandate specifies MySQL explicitly; PostgreSQL was
considered and rejected.

## Decision

**MySQL 8.4** is the only supported relational database, both locally (`docker-compose.yml`) and
in tests (Testcontainers `mysql`, see [ADR-006](ADR-006-liquibase.md)). No code path targets or
is tested against PostgreSQL or any other RDBMS.

## Consequences

- Schema types are chosen to match MySQL8's Hibernate dialect inference exactly, not generic SQL:
  `VARCHAR(n)` (not `CHAR(n)`) for `String` fields, `BIT` (not `BOOLEAN`, which MySQL treats as a
  `TINYINT` alias that Hibernate's MySQL8 dialect does not infer for `boolean` fields) for boolean
  columns. `DECIMAL(x,y)` column types must be quoted in Liquibase YAML changelogs — an unquoted
  comma inside `type: DECIMAL(19,4)` breaks YAML flow-mapping parsing.
- `hibernate.hbm2ddl.auto=validate` only ([ADR-006](ADR-006-liquibase.md)) — Hibernate never
  creates or alters schema, so any type mismatch between a changelog and an entity fails fast at
  boot rather than silently succeeding with a different column type than intended.
- No JSON/array-column features specific to PostgreSQL are used anywhere in the schema design.
