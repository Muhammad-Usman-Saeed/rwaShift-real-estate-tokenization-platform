import { apiRequest } from "@/lib/api/client";
import type { KycCaseResponse } from "@/lib/api/types";

export const kycApi = {
  submit: (investorId: string, token?: string) =>
    apiRequest<KycCaseResponse>(`/kyc/investors/${investorId}/submit`, { method: "POST", token }),
  startReview: (kycCaseId: string, token?: string) =>
    apiRequest<KycCaseResponse>(`/kyc/cases/${kycCaseId}/start-review`, { method: "POST", token }),
  verify: (kycCaseId: string, token?: string) =>
    apiRequest<KycCaseResponse>(`/kyc/cases/${kycCaseId}/verify`, { method: "POST", token }),
  reject: (kycCaseId: string, reason: string, token?: string) =>
    apiRequest<KycCaseResponse>(`/kyc/cases/${kycCaseId}/reject`, { method: "POST", body: { reason }, token }),
  getByInvestor: (investorId: string, token?: string) =>
    apiRequest<KycCaseResponse>(`/kyc/investors/${investorId}`, { token }),
  /** Compliance/admin review queue. */
  listCases: (token?: string) => apiRequest<KycCaseResponse[]>("/kyc/cases", { token }),
};
