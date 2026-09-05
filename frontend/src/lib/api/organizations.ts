import { apiRequest } from "@/lib/api/client";
import type { OrganizationResponse } from "@/lib/api/types";

export interface CreateOrganizationPayload {
  legalName: string;
  displayName: string;
  countryCode?: string;
}

export interface UpdateOrganizationPayload {
  legalName: string;
  displayName: string;
}

export const organizationsApi = {
  list: (token?: string) => apiRequest<OrganizationResponse[]>("/organizations", { token }),
  get: (id: string, token?: string) => apiRequest<OrganizationResponse>(`/organizations/${id}`, { token }),
  create: (payload: CreateOrganizationPayload, token?: string) =>
    apiRequest<OrganizationResponse>("/organizations", { method: "POST", body: payload, token }),
  update: (id: string, payload: UpdateOrganizationPayload, token?: string) =>
    apiRequest<OrganizationResponse>(`/organizations/${id}`, { method: "PUT", body: payload, token }),
  remove: (id: string, token?: string) => apiRequest<void>(`/organizations/${id}`, { method: "DELETE", token }),
};
