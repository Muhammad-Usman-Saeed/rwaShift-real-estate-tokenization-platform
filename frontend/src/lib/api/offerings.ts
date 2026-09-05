import { apiRequest } from "@/lib/api/client";
import type { OfferingResponse } from "@/lib/api/types";

export interface CreateOfferingPayload {
  assetId: string;
  legalStructureId: string;
  name: string;
  targetRaise: number;
  currency: string;
  totalUnits: number;
  unitPrice: number;
  minimumInvestment: number;
  offeredInterestPercentage: number;
  openingDate?: string;
  closingDate?: string;
}

export type UpdateOfferingPayload = Omit<CreateOfferingPayload, "assetId" | "legalStructureId">;

export const offeringsApi = {
  list: (token?: string) => apiRequest<OfferingResponse[]>("/offerings", { token }),
  get: (id: string, token?: string) => apiRequest<OfferingResponse>(`/offerings/${id}`, { token }),
  create: (payload: CreateOfferingPayload, token?: string) =>
    apiRequest<OfferingResponse>("/offerings", { method: "POST", body: payload, token }),
  update: (id: string, payload: UpdateOfferingPayload, token?: string) =>
    apiRequest<OfferingResponse>(`/offerings/${id}`, { method: "PUT", body: payload, token }),
  submitForReview: (id: string, token?: string) =>
    apiRequest<OfferingResponse>(`/offerings/${id}/submit-for-review`, { method: "POST", token }),
  approve: (id: string, token?: string) => apiRequest<OfferingResponse>(`/offerings/${id}/approve`, { method: "POST", token }),
  reject: (id: string, token?: string) => apiRequest<OfferingResponse>(`/offerings/${id}/reject`, { method: "POST", token }),
  beginTokenization: (id: string, token?: string) =>
    apiRequest<OfferingResponse>(`/offerings/${id}/begin-tokenization`, { method: "POST", token }),
  close: (id: string, token?: string) => apiRequest<OfferingResponse>(`/offerings/${id}/close`, { method: "POST", token }),
};
