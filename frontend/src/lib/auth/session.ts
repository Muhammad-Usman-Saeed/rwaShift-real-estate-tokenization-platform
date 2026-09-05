"use client";

import { useSession } from "next-auth/react";

/**
 * The bearer token every capability client attaches as `Authorization: Bearer <token>`. Never
 * cached outside `next-auth`'s own session state — this hook is the only sanctioned way a
 * component reads it.
 */
export function useAccessToken(): string | undefined {
  const { data } = useSession();
  return data?.accessToken;
}

export interface CurrentUser {
  isLoading: boolean;
  isAuthenticated: boolean;
  userId?: string;
  roles: string[];
  organizationId: string | null;
  investorId: string | null;
  sessionError?: "RefreshAccessTokenError";
}

export function useCurrentUser(): CurrentUser {
  const { data, status } = useSession();
  return {
    isLoading: status === "loading",
    isAuthenticated: status === "authenticated",
    userId: data?.user?.id,
    roles: data?.roles ?? [],
    organizationId: data?.organizationId ?? null,
    investorId: data?.investorId ?? null,
    sessionError: data?.error,
  };
}
