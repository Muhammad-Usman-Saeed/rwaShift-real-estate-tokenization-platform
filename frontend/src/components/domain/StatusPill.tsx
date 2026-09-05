import { Badge, BadgeDot } from "@/components/ui/Badge";
import { cn } from "@/lib/utils/cn";

type Tone = "neutral" | "brand" | "gold" | "success" | "warning" | "danger" | "info";

interface StatusConfig {
  label: string;
  tone: Tone;
}

/**
 * Every status vocabulary in the platform, mapped to a stable label + tone. Status is always
 * conveyed by the text label (and the dot's shape/position), never by color alone — see the
 * product spec's accessibility requirement.
 */
const STATUS_CONFIG: Record<string, StatusConfig> = {
  // Offering lifecycle
  DRAFT: { label: "Draft", tone: "neutral" },
  UNDER_REVIEW: { label: "Under Review", tone: "info" },
  APPROVED: { label: "Approved", tone: "brand" },
  REJECTED: { label: "Rejected", tone: "danger" },
  TOKENIZING: { label: "Tokenizing", tone: "gold" },
  OPEN: { label: "Open", tone: "success" },
  FUNDED: { label: "Funded", tone: "success" },
  CLOSED: { label: "Closed", tone: "neutral" },

  // Asset
  UNDER_VERIFICATION: { label: "Under Verification", tone: "info" },
  VERIFIED: { label: "Verified", tone: "success" },

  // Legal structure
  ACTIVE: { label: "Active", tone: "success" },

  // Investor
  SUSPENDED: { label: "Suspended", tone: "danger" },

  // KYC
  NOT_STARTED: { label: "Not Started", tone: "neutral" },
  SUBMITTED: { label: "Submitted", tone: "info" },

  // Eligibility
  ELIGIBLE: { label: "Eligible", tone: "success" },
  INELIGIBLE: { label: "Ineligible", tone: "danger" },
  PENDING: { label: "Pending", tone: "warning" },

  // Investment
  INITIATED: { label: "Initiated", tone: "neutral" },
  ELIGIBILITY_PENDING: { label: "Eligibility Pending", tone: "warning" },
  ELIGIBILITY_REJECTED: { label: "Eligibility Rejected", tone: "danger" },
  PAYMENT_PENDING: { label: "Payment Pending", tone: "warning" },
  CONFIRMED: { label: "Confirmed", tone: "success" },
  PAYMENT_FAILED: { label: "Payment Failed", tone: "danger" },
  TOKEN_ISSUANCE_PENDING: { label: "Issuing Units", tone: "gold" },
  SETTLED: { label: "Settled", tone: "success" },
  TOKEN_ISSUANCE_FAILED: { label: "Issuance Failed", tone: "danger" },
  CANCELLED: { label: "Cancelled", tone: "neutral" },

  // Blockchain transaction
  CREATED: { label: "Created", tone: "neutral" },
  FAILED: { label: "Failed", tone: "danger" },

  // Distribution
  CALCULATED: { label: "Calculated", tone: "info" },
  PROCESSING: { label: "Processing", tone: "gold" },
  COMPLETED: { label: "Completed", tone: "success" },

  // Deployment
};

export function StatusPill({ status, className }: { status: string; className?: string }) {
  const config = STATUS_CONFIG[status] ?? { label: status.replaceAll("_", " "), tone: "neutral" as Tone };
  return (
    <Badge tone={config.tone} className={cn(className)}>
      <BadgeDot tone={config.tone} />
      {config.label}
    </Badge>
  );
}
