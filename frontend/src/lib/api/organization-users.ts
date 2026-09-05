import { apiRequest } from "@/lib/api/client";

export type OrganizationUserRole = "ORGANIZATION_ADMIN" | "ISSUER_ADMIN" | "ISSUER_OPERATOR" | "COMPLIANCE_OFFICER";

export interface OrganizationUserResponse {
  userId: string;
  email: string;
  displayName: string;
  role: string;
  status: string;
}

export interface CreateOrganizationUserResponse extends OrganizationUserResponse {
  temporaryPassword: string;
}

export interface CreateOrganizationUserPayload {
  email: string;
  displayName: string;
  role: OrganizationUserRole;
}

export const organizationUsersApi = {
  list: (organizationId: string, token?: string) =>
    apiRequest<OrganizationUserResponse[]>(`/organizations/${organizationId}/users`, { token }),
  create: (organizationId: string, payload: CreateOrganizationUserPayload, token?: string) =>
    apiRequest<CreateOrganizationUserResponse>(`/organizations/${organizationId}/users`, {
      method: "POST",
      body: payload,
      token,
    }),
};
