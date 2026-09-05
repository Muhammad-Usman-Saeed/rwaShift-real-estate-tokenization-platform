import { apiRequest } from "@/lib/api/client";
import type { NetworkStatusResponse } from "@/lib/api/types";

/** Unauthenticated — safe to call before sign-in (see offchain's `/api/v1/public/**` matcher). */
export const networkApi = {
  status: () => apiRequest<NetworkStatusResponse>("/public/network-status"),
};
