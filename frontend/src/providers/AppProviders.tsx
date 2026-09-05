"use client";

import type { ReactNode } from "react";
import type { Session } from "next-auth";
import { SessionProvider } from "next-auth/react";
import { QueryProvider } from "@/providers/QueryProvider";
import { WalletProvider } from "@/providers/WalletProvider";
import { ToastProvider } from "@/components/ui/Toast";
import { SessionWatcher } from "@/providers/SessionWatcher";

/**
 * `session` is the server-resolved session from `RootLayout` (via `auth()`) — seeding
 * `SessionProvider` with it makes the client's first hydration pass already agree with what the
 * server rendered (e.g. the topbar's user/roles). Without it, the client starts from `status:
 * "loading"` while the server-rendered HTML already reflects a real session (most visible right
 * after login), which is a text mismatch — React hydration errors #425/#418/#423.
 */
export function AppProviders({ children, session }: { children: ReactNode; session: Session | null }) {
  return (
    <SessionProvider session={session} refetchOnWindowFocus={false} refetchInterval={60}>
      <SessionWatcher />
      <QueryProvider>
        <WalletProvider>
          <ToastProvider>{children}</ToastProvider>
        </WalletProvider>
      </QueryProvider>
    </SessionProvider>
  );
}
