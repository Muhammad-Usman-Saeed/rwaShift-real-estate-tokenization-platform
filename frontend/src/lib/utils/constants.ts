export const ROLES = {
  PLATFORM_ADMIN: "PLATFORM_ADMIN",
  ORGANIZATION_ADMIN: "ORGANIZATION_ADMIN",
  ISSUER_ADMIN: "ISSUER_ADMIN",
  ISSUER_OPERATOR: "ISSUER_OPERATOR",
  COMPLIANCE_OFFICER: "COMPLIANCE_OFFICER",
  INVESTOR: "INVESTOR",
} as const;

export type Role = (typeof ROLES)[keyof typeof ROLES];

/** Roles that see the Issuer Portal (organization-scoped issuer-side staff). */
export const ISSUER_ROLES: Role[] = [
  ROLES.ORGANIZATION_ADMIN,
  ROLES.ISSUER_ADMIN,
  ROLES.ISSUER_OPERATOR,
  ROLES.COMPLIANCE_OFFICER,
];

/** Roles that see the Platform Admin portal. Compliance officers also get admin-side KYC/compliance screens. */
export const ADMIN_ROLES: Role[] = [ROLES.PLATFORM_ADMIN, ROLES.COMPLIANCE_OFFICER];

export const INVESTOR_ROLES: Role[] = [ROLES.INVESTOR];

export const PORTAL_HOME: Record<"issuer" | "investor" | "admin", string> = {
  issuer: "/issuer/dashboard",
  investor: "/investor/opportunities",
  admin: "/admin/dashboard",
};

export const CHAINS = {
  anvil: { id: 31337, name: "Anvil (Local)" },
  sepolia: { id: 11155111, name: "Sepolia" },
} as const;

export const OFFERING_STATUS_ORDER = [
  "DRAFT",
  "UNDER_REVIEW",
  "APPROVED",
  "TOKENIZING",
  "OPEN",
  "FUNDED",
  "CLOSED",
] as const;

export const BLOCKCHAIN_TX_STATUS_ORDER = ["CREATED", "SUBMITTED", "PENDING", "CONFIRMED", "FAILED"] as const;

export const ZERO_ADDRESS = "0x0000000000000000000000000000000000000000";
