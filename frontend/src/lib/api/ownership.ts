import { apiRequest } from "@/lib/api/client";
import type { OwnershipRecordResponse } from "@/lib/api/types";

export const ownershipApi = {
  forOffering: (offeringId: string, token?: string) =>
    apiRequest<OwnershipRecordResponse[]>(`/ownership/offerings/${offeringId}`, { token }),
  forInvestor: (investorId: string, token?: string) =>
    apiRequest<OwnershipRecordResponse[]>(`/ownership/investors/${investorId}`, { token }),
};
