import { apiRequest } from "@/lib/api/client";
import type { AuditLogEntryResponse } from "@/lib/api/types";

export const auditApi = {
  /** Platform admins get the platform-wide trail; other roles get their own organization's. */
  list: (token?: string) => apiRequest<AuditLogEntryResponse[]>("/audit", { token }),
  forResource: (resourceType: string, resourceId: string, token?: string) =>
    apiRequest<AuditLogEntryResponse[]>(`/audit/resources/${resourceType}/${resourceId}`, { token }),
};
