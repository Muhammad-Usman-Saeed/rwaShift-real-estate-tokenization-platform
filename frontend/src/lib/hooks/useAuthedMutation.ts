"use client";

import { useMutation, type UseMutationOptions } from "@tanstack/react-query";
import { useAccessToken } from "@/lib/auth/session";

export function useAuthedMutation<TData, TVariables = void>(
  mutationFn: (token: string, variables: TVariables) => Promise<TData>,
  options?: Omit<UseMutationOptions<TData, unknown, TVariables>, "mutationFn">,
) {
  const token = useAccessToken();
  return useMutation({
    mutationFn: (variables: TVariables) => mutationFn(token as string, variables),
    ...options,
  });
}
