# C4 — Level 2: Container

```mermaid
C4Container
    title RWA Shift Platform — Container Diagram

    Person(user, "Issuer / Compliance / Investor / Platform Admin")

    System_Boundary(platformBoundary, "RWA Shift Platform") {
        Container(app, "rwashift-platform", "Spring Boot 3.3 / Java 21", "Modular monolith: 16 capability units, REST API, OAuth2/OIDC Authorization Server, scheduled blockchain-transaction poller and event indexer")
        ContainerDb(mysql, "MySQL 8.4", "Relational database", "All off-chain state: organizations, assets, offerings, investors, KYC, investments, blockchain-transaction bookkeeping, ownership projection, distributions, documents, audit log, OAuth2 client/authorization state")
        Container(docs, "Document Storage", "Filesystem volume", "Content-addressed (SHA-256) storage for KYC/legal documents, behind the DocumentStorage port")
    }

    System_Ext(chain, "On-Chain Layer", "ERC-3643 / T-REX contracts", "Anvil (local) / Sepolia / mainnet")
    System_Ext(otelCollector, "OTel Collector", "otel-collector-contrib")

    Rel(user, app, "HTTPS/REST + OIDC login", "JSON")
    Rel(app, mysql, "Reads/writes", "JDBC")
    Rel(app, docs, "Stores/retrieves documents", "Filesystem I/O")
    Rel(app, chain, "eth_call / eth_sendTransaction (Tokenization unit only)", "JSON-RPC via Web3j")
    Rel(app, otelCollector, "Exports traces/metrics", "OTLP/HTTP :4318")
```

## Notes

- **One deployable** (`rwashift-platform`), per [ADR-003](../adr/ADR-003-modular-monolith.md) —
  the "containers" here are the app, its database, its document store, and the two external
  systems it talks to, not a fleet of internally-decomposed services.
- `docker-compose.yml` additionally runs an `anvil` container for local development so the full
  stack (app + MySQL + a real chain + observability collector) is reproducible with one command
  — see the top-level [README](../../README.md#running-locally).
- Document Storage is a filesystem volume in V1 (`FilesystemDocumentStorage`); the `DocumentStorage`
  port is the seam for swapping in object storage (S3-compatible) later without touching the
  `document` unit's application logic.
