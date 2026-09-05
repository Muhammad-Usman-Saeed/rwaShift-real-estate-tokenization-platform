# RWA Shift Real Estate — Off-Chain Platform

Java 21 / Spring Boot 3.3 modular monolith implementing everything in **RWA Shift Real Estate**
that is *not* the ERC-3643 token contracts themselves: organizations, assets, legal structures,
offerings, investors, KYC, compliance eligibility, investments/payments, the blockchain
transaction lifecycle and event indexer, an ownership read-model, distributions, documents,
reporting, and audit. The on-chain layer (ERC-3643/T-REX composition, `RwaShiftTokenFactory`,
`RwaShiftIdentityGateway`, `RwaShiftDistributionRegistry`) lives in the sibling
[`onchain/`](../onchain/README.md) Foundry project and is treated here as an external system
reached only through the **Tokenization** capability.

## Why a modular monolith, and why capability units

One deployable, one database, one JVM — but internally partitioned into 16 **capability units**,
each a self-contained hexagonal slice (`api` → `application` → `domain` → `infrastructure`).
Units never reach into each other's JPA entities or repositories; all cross-unit calls go through
an explicit `application/port` interface the *called* unit defines and the *caller* injects.
Rationale, alternatives considered, and consequences are recorded in
[ADR-001](docs/adr/ADR-001-capability-based-architecture.md),
[ADR-002](docs/adr/ADR-002-composite-unit-structure.md), and
[ADR-003](docs/adr/ADR-003-modular-monolith.md).

## Capability map

| Unit | Responsibility | Key ports it exposes |
|---|---|---|
| `organization` | Issuer organizations (tenants) | — |
| `iam` | Platform users, org membership, OAuth2/OIDC issuance | `InvestorLinkPort` |
| `asset` | Physical property records | `AssetLookupPort` |
| `legalstructure` | SPV / legal-entity wrapper around an asset | `LegalStructureLookupPort` |
| `offering` | Fundraise lifecycle (DRAFT → ... → OPEN → FUNDED/CLOSED) | `OfferingLookupPort`, `OfferingTokenizationCallbackPort` |
| `investor` | Platform-wide investor profile + linked wallets | `InvestorLookupPort` |
| `kyc` | KYC case lifecycle (simulated provider) | `KycLookupPort` |
| `compliance` | Eligibility rule evaluation, triggers identity registration | `EligibilityLookupPort` |
| `investment` | Investment + payment lifecycle, triggers token issuance | `TokenIssuanceCallbackPort` |
| `tokenization` | **Only** unit allowed to touch Web3j / the chain | `IdentityRegistrationPort`, `TokenIssuancePort`, `DistributionRecordingPort` |
| `ownership` | Read-model projection of on-chain balances (never authoritative) | `OwnershipProjectionPort`, `OwnershipLookupPort` |
| `distribution` | Pro-rata distribution calculation + on-chain anchoring | — |
| `document` | Content-addressed document storage abstraction | — |
| `reporting` | Cross-unit read aggregation (via other units' application services) | — |
| `audit` | Append-only audit log | `AuditPort` |
| `notification` | Minimal notification sender (logs only in V1) | — |

Full responsibility/trust/upgradeability/security-boundary write-ups for the two most
architecturally significant units are in [ADR-007](docs/adr/ADR-007-blockchain-source-of-truth-boundary.md)
(Tokenization/Ownership boundary) and [ADR-010](docs/adr/ADR-010-blockchain-transaction-lifecycle.md)
(the transaction state machine). See [docs/c4](docs/c4/) for diagrams.

## The chain is the source of truth for ownership; MySQL is the source of truth for everything else

`ownership.OwnershipRecord` is explicitly documented as a **projection**, rebuilt from
`Transfer` events indexed by `BlockchainEventIndexer`. It is never written to directly by any
other unit and is never used to authorize a transfer (the chain itself does that via T-REX's
`IdentityRegistry.isVerified`). Every other capability's data (organizations, assets, legal
structures, offering metadata, investor PII, KYC decisions, documents, audit trail) lives only in
MySQL and never touches the chain. See [ADR-007](docs/adr/ADR-007-blockchain-source-of-truth-boundary.md)
and [ADR-008](docs/adr/ADR-008-offchain-pii.md).

## Blockchain transaction lifecycle

Every write to the chain — offering token deployment, investor identity registration, unit
issuance, distribution recording — goes through `tokenization.BlockchainTransactionManager`,
which drives an explicit state machine:

```
CREATED → SUBMITTED → PENDING → CONFIRMED
                            └──→ FAILED
```

`@Scheduled pollOutstandingTransactions()` polls receipts and, on confirmation, calls back into
the originating unit (`TokenIssuanceCallbackPort` for investments, `OfferingTokenizationCallbackPort`
for offerings — both `@Lazy`-injected to avoid a circular bean graph). Offering token deployment is
two-phase: `createOffering` confirms, then `BlockchainTransactionManager` auto-submits the
follow-up `unpause` call before the offering is reported tokenized. See
[ADR-010](docs/adr/ADR-010-blockchain-transaction-lifecycle.md).

`BlockchainEventIndexer.indexAllTokens()` separately polls `Transfer` logs for every confirmed
token deployment and applies them to the Ownership projection, idempotent on `(txHash, logIndex)`.

## Multi-tenancy

Every organization-scoped entity extends `TenantScopedEntity` (an `organizationId` column).
`TenantContext` (`userId, organizationId, roles, investorId`) is resolved per-request from the JWT
and passed explicitly into every tenant-scoped application-service method; `requireSameOrganization()`
throws `TenantAccessDeniedException` (mapped to HTTP 403) on mismatch. `PLATFORM_ADMIN` is the only
role exempt from the check. Verified by `TenantIsolationIT`. Investors are **not** tenant-scoped —
one investor profile can hold eligibility/positions across multiple issuers' offerings.

## Identity & access management

Spring Authorization Server issues OAuth2/OIDC tokens for the platform's own `rwashift-web` client
(registered idempotently at boot by `OAuth2ClientBootstrap`). Two `SecurityFilterChain`s: `@Order(1)`
serves the Authorization Server endpoints (`/oauth2/*`, `/.well-known/*`); `@Order(2)` is the
resource server (JWT bearer auth) for `/api/**` plus form login for the AS's own login page. Access
tokens carry custom claims — `org_id`, `roles`, `investor_id` — added by `TokenClaimsCustomizer` and
read back into `TenantContext` per request. See [ADR-009](docs/adr/ADR-009-spring-authorization-server.md).

Roles: `PLATFORM_ADMIN`, `ORGANIZATION_ADMIN`, `ISSUER_ADMIN`, `ISSUER_OPERATOR`,
`COMPLIANCE_OFFICER`, `INVESTOR`.

## Running locally

### Full stack (Docker Compose)

```bash
docker compose up --build
```

Starts MySQL 8.4, an Anvil dev chain, an OTel collector, and the app on `:8080`. The app boots with
empty on-chain contract addresses (`TOKEN_FACTORY_ADDRESS` etc. unset) until you deploy the
`onchain/` contracts against the same Anvil instance and export the addresses from
`onchain/deployments/local.json`:

```bash
cd ../onchain
anvil   # if not already using the compose-managed one — point RPC at localhost:8545 either way
forge script script/DeployLocal.s.sol:DeployLocal --rpc-url http://localhost:8545 --broadcast

export TOKEN_FACTORY_ADDRESS=<from deployments/local.json>
export IDENTITY_GATEWAY_ADDRESS=<from deployments/local.json>
export DISTRIBUTION_REGISTRY_ADDRESS=<from deployments/local.json>
cd ../offchain
docker compose up --build
```

### App only (Maven, against an already-running MySQL/Anvil)

```bash
./mvnw spring-boot:run
```

Reads `DB_HOST`, `DB_PORT`, `BLOCKCHAIN_RPC_URL`, etc. from the environment (defaults in
`application.yml` assume `localhost`).

### Demo data

Set `RWASHIFT_SEED_ENABLED=true` (or `rwashift.seed.enabled=true`) to run `DemoDataSeeder` on boot.
Seeds the exact acceptance scenario: **ABC Real Estate** issuer, **Dubai Business Tower** asset
($10,000,000), **Dubai Business Tower SPV Ltd.**, a $2,000,000 / 20,000-unit / $100 offering ($5,000
minimum), **Investor A** (KYC-verified) and **Investor B** (deliberately left unverified). The
seeder stops at `APPROVED` — completing tokenization requires the `onchain/` contracts already
deployed on the target chain. Next steps are logged on boot:

```
POST /api/v1/offerings/{id}/begin-tokenization
POST /api/v1/tokenization/offerings/{id}/deploy
POST /api/v1/investments  ... then confirm payment
```

Never enabled during `mvn test` (`AbstractIntegrationTest` forces `rwashift.seed.enabled=false`).

## Testing

```bash
./mvnw test
```

27 tests: domain-model unit tests (state machines for `Offering`, `KycCase`, `Investment`,
`TenantContext`), a Mockito-based capability unit test (`ComplianceApplicationServiceTest`, with
`IdentityRegistrationPort`/`AuditPort` mocked so eligibility-rule logic is verified without a live
chain), and Testcontainers-backed integration tests: `TenantIsolationIT` (cross-organization access
is denied) and `OffChainLifecycleIT` (organization → asset → legal structure → offering → approve →
tokenization-confirmed → investor → KYC → audit trail, as far as the flow can go without a live
chain — see the class Javadoc for the exact scope boundary). `AbstractIntegrationTest` uses a
singleton Testcontainers MySQL instance shared across the JVM.

On-chain behavior (identity registration, unit issuance, ownership projection, distribution
settlement) is covered by the separate `onchain/` Foundry suite plus manual verification of
`TokenizationApplicationService`/`BlockchainTransactionManager` against a real Anvil instance.

## API documentation

OpenAPI/Swagger UI at `/swagger-ui.html`, raw spec at `/v3/api-docs`, once the app is running.
Errors follow RFC 9457 Problem Details (`GlobalExceptionHandler`); every response/log line carries
a correlation ID (`CorrelationIdFilter`) and, once traced, `traceId`/`spanId`.

## Observability

OpenTelemetry auto-instrumentation (`opentelemetry-spring-boot-starter`) exports traces/metrics via
OTLP/HTTP to the `otel-collector` service on `:4318`. See [ADR-011](docs/adr/ADR-011-opentelemetry.md).

## Architecture documentation

- [docs/c4](docs/c4/) — System Context, Container, and Component diagrams.
- [docs/adr](docs/adr/) — ADR-001 through ADR-012, covering architecture style, persistence,
  blockchain integration, IAM, and observability decisions.
