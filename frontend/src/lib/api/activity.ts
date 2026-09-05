import { apiRequest } from "@/lib/api/client";

export interface ActivityFeedItem {
  id: string;
  action: string;
  resourceType: string;
  message: string;
  occurredAt: string;
}

export interface ActivityStatsResponse {
  totalCount: number;
  countByCategory: Record<string, number>;
}

export interface VisitStatsResponse {
  visitorsToday: number;
  visitorsThisWeek: number;
  visitorsThisMonth: number;
  visitorsThisYear: number;
  investorsToday: number;
  investorsThisWeek: number;
  investorsThisMonth: number;
  investorsThisYear: number;
}

export const activityApi = {
  feed: (limit = 50, token?: string) => apiRequest<ActivityFeedItem[]>(`/activity/feed?limit=${limit}`, { token }),
  stats: (token?: string) => apiRequest<ActivityStatsResponse>("/activity/stats", { token }),
  visitStats: (token?: string) => apiRequest<VisitStatsResponse>("/activity/visit-stats", { token }),
  recordVisit: (token?: string) => apiRequest<void>("/activity/visits", { method: "POST", token }),
};
