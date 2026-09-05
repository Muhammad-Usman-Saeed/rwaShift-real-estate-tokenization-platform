import { apiRequest } from "@/lib/api/client";

export interface NotificationItem {
  id: string;
  type: string;
  title: string;
  message: string;
  link: string;
  occurredAt: string;
}

export const notificationsApi = {
  list: (token?: string) => apiRequest<NotificationItem[]>("/notifications", { token }),
};
