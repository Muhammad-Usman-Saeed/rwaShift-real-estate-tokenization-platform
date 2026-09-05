"use client";

import { useEffect } from "react";
import { useSession } from "next-auth/react";
import { rpInitiatedSignOut } from "@/lib/auth/actions";
import { logger } from "@/lib/utils/logger";

/**
 * `auth.ts`'s `jwt` callback proactively refreshes the access token ~60s before expiry; if that
 * refresh call itself fails (refresh token expired/revoked, Authorization Server restarted with a
 * lost session, etc.) it sets `session.error = "RefreshAccessTokenError"` and otherwise leaves the
 * stale token in place — every subsequent API call would then 401 in a loop with no way for the
 * user to recover except manually finding the sign-out button. This watches for that error and
 * forces a full sign-out straight to `/login` instead, so a broken refresh reads as "please log
 * back in" rather than a silently broken app.
 */
export function SessionWatcher() {
  const { data: session } = useSession();

  useEffect(() => {
    if (session?.error === "RefreshAccessTokenError") {
      logger.warn("session refresh failed, signing out", { error: session.error });
      void rpInitiatedSignOut();
    }
  }, [session?.error]);

  return null;
}
