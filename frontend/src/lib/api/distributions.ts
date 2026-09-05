import { apiRequest } from "@/lib/api/client";
import type { DistributionEntitlementResponse, DistributionResponse } from "@/lib/api/types";

export interface CreateDistributionPayload {
  offeringId: string;
  totalAmount: number;
  currency: string;
  recordDate: string;
}

export const distributionsApi = {
  create: (payload: CreateDistributionPayload, token?: string) =>
    apiRequest<DistributionResponse>("/distributions", { method: "POST", body: payload, token }),
  calculate: (id: string, token?: string) =>
    apiRequest<DistributionResponse>(`/distributions/${id}/calculate`, { method: "POST", token }),
  approve: (id: string, token?: string) =>
    apiRequest<DistributionResponse>(`/distributions/${id}/approve`, { method: "POST", token }),
  process: (id: string, token?: string) =>
    apiRequest<DistributionResponse>(`/distributions/${id}/process`, { method: "POST", token }),
  complete: (id: string, token?: string) =>
    apiRequest<DistributionResponse>(`/distributions/${id}/complete`, { method: "POST", token }),
  get: (id: string, token?: string) => apiRequest<DistributionResponse>(`/distributions/${id}`, { token }),
  entitlements: (id: string, token?: string) =>
    apiRequest<DistributionEntitlementResponse[]>(`/distributions/${id}/entitlements`, { token }),
  list: (token?: string) => apiRequest<DistributionResponse[]>("/distributions", { token }),
  forOffering: (offeringId: string, token?: string) =>
    apiRequest<DistributionResponse[]>(`/distributions/offerings/${offeringId}`, { token }),
};
