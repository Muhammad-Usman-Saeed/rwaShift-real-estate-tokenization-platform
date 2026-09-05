# C4 — Level 1: System Context

```mermaid
C4Context
    title RWA Shift Real Estate — System Context

    Person(issuerAdmin, "Issuer Admin", "Manages assets, legal structures, and offerings for their organization")
    Person(complianceOfficer, "Compliance Officer", "Verifies assets, approves offerings, reviews KYC, approves distributions")
    Person(investor, "Investor", "Onboards, completes KYC, invests in offerings, views holdings")
    Person(platformAdmin, "Platform Admin", "Operates the platform across all organizations")

    System(platform, "RWA Shift Platform", "Java 21 / Spring Boot 3.3 modular monolith. Owns organizations, assets, offerings, investors, KYC, compliance, investments, and orchestrates on-chain tokenization.")

    System_Ext(chain, "RWA Shift Real Estate — On-Chain Layer", "ERC-3643 (T-REX) contracts on Ethereum-compatible chain (Anvil/Sepolia). Enforces transfer eligibility, holds token balances, records distributions.")
    System_Ext(kycProvider, "KYC Provider", "Simulated in V1 (DemoKycProvider); a real external KYC/AML vendor in production.")
    System_Ext(paymentProvider, "Payment Provider", "Simulated in V1 (DemoPaymentProvider); a real payment rail in production.")
    System_Ext(otel, "Observability Backend", "OTel Collector + downstream tracing/metrics backend")

    Rel(issuerAdmin, platform, "Manages assets/offerings via", "HTTPS/REST")
    Rel(complianceOfficer, platform, "Reviews/approves via", "HTTPS/REST")
    Rel(investor, platform, "Invests, views holdings via", "HTTPS/REST")
    Rel(platformAdmin, platform, "Administers via", "HTTPS/REST")

    Rel(platform, chain, "Deploys tokens, registers identities, issues units, records distributions via", "JSON-RPC (Web3j), Tokenization unit only")
    Rel(platform, kycProvider, "Submits KYC decisions via", "Provider port")
    Rel(platform, paymentProvider, "Confirms payments via", "Provider port")
    Rel(platform, otel, "Exports traces/metrics via", "OTLP/HTTP")
```

## Notes

- The platform is the only system investors, issuer staff, and compliance officers interact with
  directly. They never interact with the chain or with the KYC/payment providers directly.
- The on-chain layer ([`onchain/`](../../../onchain/README.md)) is modeled as an external system
  from the off-chain platform's point of view, even though both are developed in this same
  repository — the platform only ever reaches it through the `tokenization` unit's Web3j gateway,
  never by any other path (see [ADR-007](../adr/ADR-007-blockchain-source-of-truth-boundary.md)).
- KYC and payment providers are pluggable ports (`KycProvider`, `PaymentProvider`) with
  demo/simulated implementations in V1 (`DemoKycProvider`, `DemoPaymentProvider`) — swapping in a
  real vendor is an infrastructure-layer adapter change, not a domain change.
