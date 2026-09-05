import { apiRequest } from "@/lib/api/client";
import type { CurrentUserResponse } from "@/lib/api/types";

export const iamApi = {
  me: (token?: string) => apiRequest<CurrentUserResponse>("/iam/me", { token }),
};
