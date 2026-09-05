import { apiRequest } from "@/lib/api/client";
import type { EligibilityDecisionResponse } from "@/lib/api/types";

export const complianceApi = {
  evaluate: (investorId: string, offeringId: string, token?: string) =>
    apiRequest<EligibilityDecisionResponse>(
      `/compliance/eligibility?investorId=${encodeURIComponent(investorId)}&offeringId=${encodeURIComponent(offeringId)}`,
      { method: "POST", token },
    ),
  get: (investorId: string, offeringId: string, token?: string) =>
    apiRequest<EligibilityDecisionResponse>(
      `/compliance/eligibility?investorId=${encodeURIComponent(investorId)}&offeringId=${encodeURIComponent(offeringId)}`,
      { token },
    ),
};
