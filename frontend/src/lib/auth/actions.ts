"use server";

import { redirect } from "next/navigation";
import { auth, signOut } from "@/lib/auth/auth";
import { logger } from "@/lib/utils/logger";

/**
 * Plain `signOut()` only clears this app's own session cookie — the Authorization Server
 * (`offchain`) still has its own logged-in browser session, so the very next `/oauth2/authorize`
 * redirect (e.g. clicking "Sign in" again) silently re-authenticates as the same user with no
 * login prompt. That's RP-initiated logout's whole point: after clearing our cookie, send the
 * browser to the Authorization Server's OIDC `end_session_endpoint` (`/connect/logout`, exposed
 * per the OIDC discovery doc — already enabled via `.oidc(...)` in `AuthorizationServerConfig`
 * and already has `http://localhost:3000` registered as an allowed `post_logout_redirect_uri` in
 * `OAuth2ClientBootstrap`) so it invalidates its own session too. Spring renders a one-click
 * logout confirmation page first — standard, spec-recommended behavior to prevent logout CSRF,
 * not a bug.
 */
export async function rpInitiatedSignOut() {
  const session = await auth();
  const idToken = session?.idToken;
  logger.info("signing out", { userId: session?.user?.id, hadIdToken: Boolean(idToken) });

  await signOut({ redirect: false });

  const issuer = process.env.AUTH_ISSUER as string;
  const postLogoutRedirectUri = `${process.env.AUTH_URL ?? "http://localhost:3000"}/login`;
  const params = new URLSearchParams({ post_logout_redirect_uri: postLogoutRedirectUri });
  if (idToken) {
    params.set("id_token_hint", idToken);
  }
  redirect(`${issuer}/connect/logout?${params.toString()}`);
}
