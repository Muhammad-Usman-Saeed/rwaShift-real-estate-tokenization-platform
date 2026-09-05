"use client";

import { useQuery, type UseQueryOptions } from "@tanstack/react-query";
import { useAccessToken } from "@/lib/auth/session";

/**
 * Thin wrapper around `useQuery` that threads the current session's access token into the query
 * function and holds the query off until a token is available — every screen's data-fetching goes
 * through this (or `useAuthedMutation`) rather than calling `useQuery` directly, so the
 * token-gating rule can't be forgotten per call site.
 */
export function useAuthedQuery<T>(
  queryKey: readonly unknown[],
  queryFn: (token: string) => Promise<T>,
  options?: Partial<Omit<UseQueryOptions<T, unknown, T, readonly unknown[]>, "queryKey" | "queryFn">>,
) {
  const token = useAccessToken();
  return useQuery({
    queryKey,
    queryFn: () => queryFn(token as string),
    enabled: !!token && (options?.enabled ?? true),
    ...options,
  });
}
