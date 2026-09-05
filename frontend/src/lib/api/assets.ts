import { apiRequest } from "@/lib/api/client";
import type { AssetResponse, AssetType } from "@/lib/api/types";

export interface CreateAssetPayload {
  name: string;
  type: AssetType;
  description?: string;
  location: string;
  valuation: number;
  currency: string;
}

export type UpdateAssetPayload = Omit<CreateAssetPayload, "type">;

export const assetsApi = {
  list: (token?: string) => apiRequest<AssetResponse[]>("/assets", { token }),
  get: (id: string, token?: string) => apiRequest<AssetResponse>(`/assets/${id}`, { token }),
  create: (payload: CreateAssetPayload, token?: string) =>
    apiRequest<AssetResponse>("/assets", { method: "POST", body: payload, token }),
  update: (id: string, payload: UpdateAssetPayload, token?: string) =>
    apiRequest<AssetResponse>(`/assets/${id}`, { method: "PUT", body: payload, token }),
  submitForVerification: (id: string, token?: string) =>
    apiRequest<AssetResponse>(`/assets/${id}/submit-for-verification`, { method: "POST", token }),
  verify: (id: string, token?: string) => apiRequest<AssetResponse>(`/assets/${id}/verify`, { method: "POST", token }),
};
