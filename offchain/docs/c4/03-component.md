# C4 — Level 3: Component (inside `rwashift-platform`)

Shows the 16 capability units and the explicit `application/port` dependencies between them —
the only form of cross-unit coupling allowed (see
[ADR-002](../adr/ADR-002-composite-unit-structure.md)). Arrows point from consumer to the port's
owner.

```mermaid
C4Component
    title rwashift-platform — Capability Units and Ports

    Container_Boundary(app, "rwashift-platform") {
        Component(organization, "Organization", "Capability unit", "Issuer organizations (tenants)")
        Component(iam, "IAM", "Capability unit", "Users, org membership, OAuth2/OIDC (Spring Authorization Server)")
        Component(asset, "Asset", "Capability unit", "Physical property records")
        Component(legalstructure, "Legal Structure", "Capability unit", "SPV / legal-entity wrapper")
        Component(offering, "Offering", "Capability unit", "Fundraise lifecycle state machine")
        Component(investor, "Investor", "Capability unit", "Platform-wide investor profile + wallets")
        Component(kyc, "KYC", "Capability unit", "KYC case lifecycle")
        Component(compliance, "Compliance", "Capability unit", "Eligibility rule evaluation")
        Component(investment, "Investment", "Capability unit", "Investment + payment lifecycle")
        Component(tokenization, "Tokenization", "Capability unit", "ONLY unit with Web3j access; blockchain tx manager + event indexer")
        Component(ownership, "Ownership", "Capability unit", "Read-model projection of on-chain balances")
        Component(distribution, "Distribution", "Capability unit", "Pro-rata distribution calculation")
        Component(document, "Document", "Capability unit", "Document storage abstraction")
        Component(reporting, "Reporting", "Capability unit", "Cross-unit read aggregation")
        Component(audit, "Audit", "Capability unit", "Append-only audit log")
        Component(notification, "Notification", "Capability unit", "Notification sending (logs only, V1)")
    }

    Rel(iam, organization, "InvestorLinkPort consumer context", "in-process")
    Rel(asset, organization, "tenant-scoped via TenantContext", "in-process")
    Rel(legalstructure, asset, "AssetLookupPort", "in-process")
    Rel(offering, asset, "AssetLookupPort", "in-process")
    Rel(offering, legalstructure, "LegalStructureLookupPort", "in-process")
    Rel(compliance, kyc, "KycLookupPort", "in-process")
    Rel(compliance, investor, "InvestorLookupPort", "in-process")
    Rel(compliance, offering, "OfferingLookupPort", "in-process")
    Rel(compliance, tokenization, "IdentityRegistrationPort", "in-process")
    Rel(compliance, audit, "AuditPort", "in-process")
    Rel(kyc, investor, "InvestorLookupPort", "in-process")
    Rel(kyc, audit, "AuditPort", "in-process")
    Rel(investment, offering, "OfferingLookupPort", "in-process")
    Rel(investment, investor, "InvestorLookupPort", "in-process")
    Rel(investment, compliance, "EligibilityLookupPort", "in-process")
    Rel(investment, tokenization, "TokenIssuancePort", "in-process")
    Rel(investment, audit, "AuditPort", "in-process")
    Rel(tokenization, investment, "TokenIssuanceCallbackPort (async callback, @Lazy)", "in-process")
    Rel(tokenization, offering, "OfferingTokenizationCallbackPort (async callback, @Lazy)", "in-process")
    Rel(ownership, tokenization, "indexes Transfer events from", "polls via BlockchainEventIndexer")
    Rel(distribution, ownership, "OwnershipLookupPort", "in-process")
    Rel(distribution, offering, "OfferingLookupPort", "in-process")
    Rel(distribution, tokenization, "DistributionRecordingPort", "in-process")
    Rel(distribution, audit, "AuditPort", "in-process")
    Rel(reporting, offering, "query methods", "in-process")
    Rel(reporting, investment, "query methods", "in-process")
    Rel(reporting, ownership, "OwnershipLookupPort", "in-process")
    Rel(document, kyc, "attached to KYC cases", "in-process")
```

## Notes

- Every `Rel` above corresponds to an actual constructor-injected `application/port` interface in
  the code — there is no hidden coupling beyond what's diagrammed (enforced by
  [ADR-002](../adr/ADR-002-composite-unit-structure.md)'s "no cross-unit entity/repository access"
  rule).
- `Tokenization ↔ Investment` and `Tokenization ↔ Offering` are bidirectional: the domain unit
  calls into Tokenization to *request* an on-chain effect; Tokenization calls back into the domain
  unit (via `@Lazy`-injected callback ports) once `BlockchainTransactionManager` observes
  confirmation. See [ADR-010](../adr/ADR-010-blockchain-transaction-lifecycle.md).
- `Ownership`'s relationship to `Tokenization` is not a port call but an async poll
  (`BlockchainEventIndexer.indexAllTokens()` reads `ContractDeploymentRepository` rows that
  `Tokenization` owns, then queries `Transfer` logs directly via `Web3jContractGateway`) — the one
  place Ownership indirectly touches chain data, still entirely inside the Tokenization unit's
  package for the actual RPC call.
- `Notification` has no inbound `Rel`s drawn because V1's `LoggingNotificationSender` is not yet
  wired as a call target from any unit — it exists as a ready port for when a unit needs to notify
  (e.g. KYC rejection, distribution completed).
