/**
 * Centralized TanStack Query key factory. Every capability's hooks build keys from here so
 * invalidation after a mutation (e.g. approving an offering invalidates every offering list/detail
 * query) is consistent and can't drift between files.
 */
export const queryKeys = {
  organizations: {
    all: ["organizations"] as const,
    list: () => [...queryKeys.organizations.all, "list"] as const,
    detail: (id: string) => [...queryKeys.organizations.all, "detail", id] as const,
    users: (id: string) => [...queryKeys.organizations.all, "users", id] as const,
  },
  currentUser: () => ["current-user"] as const,
  assets: {
    all: ["assets"] as const,
    list: () => [...queryKeys.assets.all, "list"] as const,
    detail: (id: string) => [...queryKeys.assets.all, "detail", id] as const,
  },
  legalStructures: {
    all: ["legal-structures"] as const,
    list: () => [...queryKeys.legalStructures.all, "list"] as const,
    detail: (id: string) => [...queryKeys.legalStructures.all, "detail", id] as const,
    forAsset: (assetId: string) => [...queryKeys.legalStructures.all, "for-asset", assetId] as const,
  },
  offerings: {
    all: ["offerings"] as const,
    list: () => [...queryKeys.offerings.all, "list"] as const,
    detail: (id: string) => [...queryKeys.offerings.all, "detail", id] as const,
  },
  investors: {
    all: ["investors"] as const,
    list: () => [...queryKeys.investors.all, "list"] as const,
    detail: (id: string) => [...queryKeys.investors.all, "detail", id] as const,
    me: () => [...queryKeys.investors.all, "me"] as const,
  },
  kyc: {
    all: ["kyc"] as const,
    cases: () => [...queryKeys.kyc.all, "cases"] as const,
    forInvestor: (investorId: string) => [...queryKeys.kyc.all, "investor", investorId] as const,
  },
  compliance: {
    all: ["compliance"] as const,
    eligibility: (investorId: string, offeringId: string) => [...queryKeys.compliance.all, investorId, offeringId] as const,
  },
  investments: {
    all: ["investments"] as const,
    detail: (id: string) => [...queryKeys.investments.all, "detail", id] as const,
    mine: () => [...queryKeys.investments.all, "mine"] as const,
    forOrganization: () => [...queryKeys.investments.all, "organization"] as const,
  },
  tokenization: {
    all: ["tokenization"] as const,
    deployments: (offeringId: string) => [...queryKeys.tokenization.all, "deployments", offeringId] as const,
    allDeployments: () => [...queryKeys.tokenization.all, "deployments", "all"] as const,
    transactions: () => [...queryKeys.tokenization.all, "transactions"] as const,
    organizationWallets: () => [...queryKeys.tokenization.all, "organization-wallets"] as const,
  },
  ownership: {
    all: ["ownership"] as const,
    forOffering: (offeringId: string) => [...queryKeys.ownership.all, "offering", offeringId] as const,
    forInvestor: (investorId: string) => [...queryKeys.ownership.all, "investor", investorId] as const,
  },
  distributions: {
    all: ["distributions"] as const,
    list: () => [...queryKeys.distributions.all, "list"] as const,
    detail: (id: string) => [...queryKeys.distributions.all, "detail", id] as const,
    entitlements: (id: string) => [...queryKeys.distributions.all, "entitlements", id] as const,
  },
  documents: {
    all: ["documents"] as const,
    forResource: (resourceType: string, resourceId: string) => [...queryKeys.documents.all, resourceType, resourceId] as const,
  },
  reporting: {
    all: ["reporting"] as const,
    issuerSummary: () => [...queryKeys.reporting.all, "issuer-summary"] as const,
    investorSummary: (investorId: string) => [...queryKeys.reporting.all, "investor-summary", investorId] as const,
  },
  audit: {
    all: ["audit"] as const,
    list: () => [...queryKeys.audit.all, "list"] as const,
    forResource: (resourceType: string, resourceId: string) => [...queryKeys.audit.all, resourceType, resourceId] as const,
  },
  activity: {
    all: ["activity"] as const,
    feed: () => [...queryKeys.activity.all, "feed"] as const,
    stats: () => [...queryKeys.activity.all, "stats"] as const,
    visitStats: () => [...queryKeys.activity.all, "visit-stats"] as const,
  },
} as const;
