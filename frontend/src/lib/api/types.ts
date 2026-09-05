/**
 * DTO types mirroring the backend's `api/dto/*Response` records exactly (field-for-field) so the
 * frontend never invents shapes the API doesn't actually return. Money/decimal fields are typed
 * `number` because the backend serializes `BigDecimal` as a JSON number (Jackson default) — see
 * `lib/utils/format.ts` for why that's safe at real-estate scale.
 */

// ---- Network status (public, pre-login) ----
export interface ChainActivityItemResponse {
  eventName: string;
  contractLabel: string;
  blockNumber: number;
  txHash: string;
  blockTime: string | null;
}

export interface NetworkStatusResponse {
  reachable: boolean;
  network: string;
  chainId: number;
  latestBlockNumber: number | null;
  averageBlockTimeSeconds: number | null;
  recentActivity: ChainActivityItemResponse[];
}

// ---- Organization ----
export interface OrganizationResponse {
  id: string;
  legalName: string;
  displayName: string;
  countryCode: string;
  status: "ACTIVE" | "SUSPENDED";
  createdAt: string;
  updatedAt: string;
}

// ---- IAM ----
export interface CurrentUserResponse {
  userId: string;
  organizationId: string | null;
  roles: string[];
  investorId: string | null;
}

// ---- Asset ----
export type AssetType = "COMMERCIAL" | "RESIDENTIAL" | "INDUSTRIAL" | "MIXED_USE" | "LAND";
export type AssetStatus = "DRAFT" | "UNDER_VERIFICATION" | "VERIFIED";

export interface AssetResponse {
  id: string;
  organizationId: string;
  name: string;
  type: AssetType;
  description: string | null;
  location: string;
  valuation: number;
  currency: string;
  status: AssetStatus;
  createdAt: string;
  updatedAt: string;
}

// ---- Legal Structure ----
export type LegalEntityType = "SPV" | "TRUST" | "FUND" | "DIRECT_HOLDING";
export type RelationshipToAsset = "FULL_OWNER" | "MAJORITY_OWNER" | "MINORITY_OWNER" | "LEASEHOLD";
export type InvestmentInstrumentType = "EQUITY_UNITS" | "DEBT_NOTE" | "PREFERRED_UNITS";
export type LegalStructureStatus = "DRAFT" | "ACTIVE";

export interface LegalStructureResponse {
  id: string;
  organizationId: string;
  assetId: string;
  legalEntityName: string;
  entityType: LegalEntityType;
  jurisdiction: string;
  registrationNumber: string;
  relationshipToAsset: RelationshipToAsset;
  investmentInstrumentType: InvestmentInstrumentType;
  status: LegalStructureStatus;
}

// ---- Offering ----
export type OfferingStatus = "DRAFT" | "UNDER_REVIEW" | "APPROVED" | "REJECTED" | "TOKENIZING" | "OPEN" | "FUNDED" | "CLOSED";

export interface OfferingResponse {
  id: string;
  organizationId: string;
  assetId: string;
  legalStructureId: string;
  name: string;
  targetRaise: number;
  currency: string;
  totalUnits: number;
  unitPrice: number;
  minimumInvestment: number;
  offeredInterestPercentage: number;
  openingDate: string;
  closingDate: string;
  status: OfferingStatus;
  unitsIssued: number;
  unitsAvailable: number;
}

// ---- Investor ----
export type InvestorType = "INDIVIDUAL" | "ORGANIZATION";
export type InvestorStatus = "ACTIVE" | "SUSPENDED";

export interface InvestorResponse {
  id: string;
  userId: string;
  investorType: InvestorType;
  displayName: string;
  countryCode: string;
  primaryWalletAddress: string | null;
  status: InvestorStatus;
}

// ---- KYC ----
export type KycStatus = "NOT_STARTED" | "SUBMITTED" | "UNDER_REVIEW" | "VERIFIED" | "REJECTED";

export interface KycCaseResponse {
  id: string;
  investorId: string;
  status: KycStatus;
  providerReference: string | null;
  submittedAt: string | null;
  reviewedAt: string | null;
  reviewedBy: string | null;
  rejectionReason: string | null;
}

// ---- Compliance ----
export type EligibilityStatus = "ELIGIBLE" | "INELIGIBLE" | "PENDING";

export interface EligibilityDecisionResponse {
  investorId: string;
  offeringId: string;
  status: EligibilityStatus;
  reasonCodes: string[];
  evaluatedAt: string;
  identityRegistered: boolean;
}

// ---- Investment ----
export type InvestmentStatus =
  | "INITIATED"
  | "ELIGIBILITY_PENDING"
  | "ELIGIBILITY_REJECTED"
  | "PAYMENT_PENDING"
  | "CONFIRMED"
  | "PAYMENT_FAILED"
  | "TOKEN_ISSUANCE_PENDING"
  | "SETTLED"
  | "TOKEN_ISSUANCE_FAILED"
  | "CANCELLED";
export type PaymentStatus = "PENDING" | "CONFIRMED" | "FAILED";
export type TokenIssuanceStatus = "NOT_STARTED" | "PENDING" | "CONFIRMED" | "FAILED";

export interface InvestmentResponse {
  id: string;
  organizationId: string;
  investorId: string;
  offeringId: string;
  amount: number;
  currency: string;
  unitPrice: number;
  units: number;
  status: InvestmentStatus;
  paymentStatus: PaymentStatus;
  tokenIssuanceStatus: TokenIssuanceStatus;
  failureReason: string | null;
  paymentMethod: "BANK_TRANSFER" | "CRYPTO_WALLET";
  paymentTxHash: string | null;
  payerWalletAddress: string | null;
  createdAt: string;
  updatedAt: string;
}

// ---- Tokenization ----
export type ContractType = "TOKEN" | "IDENTITY_REGISTRY" | "MODULAR_COMPLIANCE";
export type DeploymentStatus = "PENDING" | "CONFIRMED" | "FAILED";

export interface ContractDeploymentResponse {
  offeringId: string;
  network: string;
  chainId: number;
  contractType: ContractType;
  contractAddress: string | null;
  deploymentTxHash: string | null;
  deploymentBlock: number | null;
  status: DeploymentStatus;
}

export type BusinessReferenceType = "OFFERING_DEPLOYMENT" | "IDENTITY_REGISTRATION" | "INVESTMENT_ISSUANCE" | "DISTRIBUTION_RECORD";
export type BlockchainTransactionStatus = "CREATED" | "SUBMITTED" | "PENDING" | "CONFIRMED" | "FAILED";

export interface BlockchainTransactionResponse {
  id: string;
  businessReferenceType: BusinessReferenceType;
  businessReferenceId: string;
  network: string;
  chainId: number;
  contractAddress: string | null;
  method: string;
  txHash: string | null;
  status: BlockchainTransactionStatus;
  submittedAt: string | null;
  confirmedAt: string | null;
  blockNumber: number | null;
  failureReason: string | null;
  retryCount: number;
}

export interface OrganizationWalletResponse {
  organizationId: string;
  walletAddress: string;
  balanceEth: number;
}

// ---- Ownership ----
export interface OwnershipRecordResponse {
  investorId: string | null;
  offeringId: string;
  walletAddress: string;
  tokenAddress: string;
  units: string; // BigInteger serialized as a numeric string by Jackson
  lastSyncedBlock: number;
}

// ---- Distribution ----
export type DistributionStatus = "DRAFT" | "CALCULATED" | "APPROVED" | "PROCESSING" | "COMPLETED";

export interface DistributionResponse {
  id: string;
  organizationId: string;
  offeringId: string;
  totalAmount: number;
  currency: string;
  recordDate: string;
  status: DistributionStatus;
}

export interface DistributionEntitlementResponse {
  investorId: string | null;
  walletAddress: string;
  unitsAtRecordDate: string;
  entitlementAmount: number;
}

// ---- Documents ----
export type DocumentClassification = "PUBLIC" | "ISSUER_CONFIDENTIAL" | "INVESTOR_CONFIDENTIAL" | "COMPLIANCE_RESTRICTED";

export interface DocumentMetadataResponse {
  id: string;
  resourceType: string;
  resourceId: string;
  filename: string;
  contentType: string;
  sha256: string;
  classification: DocumentClassification;
}

// ---- Reporting ----
export interface IssuerSummaryResponse {
  organizationId: string;
  totalAssets: number;
  totalAssetValuation: number;
  activeOfferings: number;
  totalCapitalRaised: number;
  distinctInvestorCount: number;
  totalUnitsIssued: number;
  totalUnitsAvailable: number;
  distributionCount: number;
}

export interface InvestorSummaryResponse {
  investorId: string;
  totalInvested: number;
  investmentCount: number;
  totalUnitsOwned: string;
  distinctOfferingsHeld: number;
}

// ---- Audit ----
export interface AuditLogEntryResponse {
  id: string;
  actorUserId: string | null;
  organizationId: string | null;
  action: string;
  resourceType: string;
  resourceId: string;
  previousState: string | null;
  newState: string | null;
  correlationId: string | null;
  blockchainTxHash: string | null;
  occurredAt: string;
}
