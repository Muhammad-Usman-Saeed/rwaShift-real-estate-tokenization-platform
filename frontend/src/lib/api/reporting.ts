import { apiRequest } from "@/lib/api/client";
import type { InvestorSummaryResponse, IssuerSummaryResponse } from "@/lib/api/types";

export const reportingApi = {
  issuerSummary: (token?: string) => apiRequest<IssuerSummaryResponse>("/reports/issuer/summary", { token }),
  investorSummary: (investorId: string, token?: string) =>
    apiRequest<InvestorSummaryResponse>(`/reports/investors/${investorId}/summary`, { token }),
};
