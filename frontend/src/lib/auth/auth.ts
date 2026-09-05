import NextAuth from "next-auth";
import type { OIDCConfig } from "next-auth/providers";
import type { JWT } from "next-auth/jwt";
import { decodeJwtPayload, type AccessTokenClaims } from "@/lib/auth/jwt";
import { logger } from "@/lib/utils/logger";

/**
 * The offchain app serves both the REST API and the Authorization Server on the same origin
 * (`AUTH_ISSUER`, e.g. `http://localhost:8080`) — the value baked into every token's `iss` claim,
 * and the only address the user's actual browser needs to reach (the `/oauth2/authorize`
 * redirect). `AUTH_INTERNAL_ISSUER` is a *separate* address for calls this server-side Node
 * process itself makes (discovery, token exchange, userinfo, jwks, refresh) — identical to
 * `AUTH_ISSUER` for plain `npm run dev`, but set to the backend's Docker network address
 * (`http://app:8080`) in `docker-compose.yml`, since "localhost" inside the frontend container
 * would otherwise resolve to the frontend container itself, not the backend.
 */
const browserIssuer = process.env.AUTH_ISSUER as string;
const internalIssuer = process.env.AUTH_INTERNAL_ISSUER || browserIssuer;

/**
 * Generic OIDC client for the platform's own Spring Authorization Server (see
 * `offchain/.../AuthorizationServerConfig` + `OAuth2ClientBootstrap`, which registers this exact
 * `rwashift-web` client id/secret/redirect-uri). PKCE is required by the backend registration
 * (`requireProofKey(true)`) — Auth.js's OIDC provider does this automatically via `checks: ["pkce"]`.
 * Every endpoint is set explicitly (rather than left to `issuer`-based discovery alone) so the
 * browser-facing and server-to-server addresses can differ — see the comment above.
 */
const rwashiftProvider: OIDCConfig<Record<string, unknown>> = {
  id: "rwashift",
  name: "rwaShift",
  type: "oidc",
  issuer: browserIssuer,
  wellKnown: `${internalIssuer}/.well-known/openid-configuration`,
  authorization: { url: `${browserIssuer}/oauth2/authorize`, params: { scope: "openid profile api.read api.write" } },
  token: `${internalIssuer}/oauth2/token`,
  userinfo: `${internalIssuer}/userinfo`,
  jwks_endpoint: `${internalIssuer}/oauth2/jwks`,
  clientId: process.env.AUTH_CLIENT_ID,
  clientSecret: process.env.AUTH_CLIENT_SECRET,
  checks: ["pkce", "state"],
};

/**
 * `roles`/`org_id`/`investor_id` are custom claims the backend's `TokenClaimsCustomizer` bakes
 * into the ACCESS token only (never the ID token) — see that class's Javadoc. Auth.js's OIDC
 * provider parses the ID token into `profile`, which won't carry these, so we decode the access
 * token directly instead. Refreshes re-decode on every rotation to pick up role/org changes.
 */
async function refreshAccessToken(token: JWT): Promise<JWT> {
  logger.debug("refreshing access token", { userId: token.userId });
  try {
    const basicAuth = Buffer.from(`${process.env.AUTH_CLIENT_ID}:${process.env.AUTH_CLIENT_SECRET}`).toString("base64");
    const response = await fetch(`${internalIssuer}/oauth2/token`, {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        Authorization: `Basic ${basicAuth}`,
      },
      body: new URLSearchParams({
        grant_type: "refresh_token",
        refresh_token: token.refreshToken ?? "",
      }),
    });
    if (!response.ok) throw new Error(`Refresh failed with status ${response.status}`);
    const refreshed = (await response.json()) as { access_token: string; refresh_token?: string; expires_in: number };
    const claims = decodeJwtPayload<AccessTokenClaims>(refreshed.access_token);
    logger.info("access token refreshed", { userId: claims.sub, expiresInSeconds: refreshed.expires_in });
    return {
      ...token,
      accessToken: refreshed.access_token,
      accessTokenExpires: Date.now() + refreshed.expires_in * 1000,
      refreshToken: refreshed.refresh_token ?? token.refreshToken,
      roles: claims.roles ?? [],
      organizationId: claims.org_id ?? null,
      investorId: claims.investor_id ?? null,
      userId: claims.sub,
      error: undefined,
    };
  } catch (cause) {
    logger.error("access token refresh failed", {
      userId: token.userId,
      error: cause instanceof Error ? cause.message : String(cause),
    });
    return { ...token, error: "RefreshAccessTokenError" };
  }
}

export const { handlers, auth, signIn, signOut } = NextAuth({
  providers: [rwashiftProvider],
  session: { strategy: "jwt" },
  pages: { signIn: "/login" },
  callbacks: {
    async jwt({ token, account, trigger }) {
      if (account?.access_token) {
        const claims = decodeJwtPayload<AccessTokenClaims>(account.access_token);
        return {
          ...token,
          accessToken: account.access_token,
          refreshToken: account.refresh_token,
          idToken: account.id_token,
          accessTokenExpires: account.expires_at ? account.expires_at * 1000 : undefined,
          roles: claims.roles ?? [],
          organizationId: claims.org_id ?? null,
          investorId: claims.investor_id ?? null,
          userId: claims.sub,
        };
      }
      // `update()` from the client (e.g. right after completing investor onboarding, which
      // changes the `investor_id` claim server-side) forces an immediate re-exchange instead of
      // waiting out the access token's remaining lifetime.
      if (trigger === "update") {
        return refreshAccessToken(token);
      }
      // 60s buffer so a request in flight doesn't race an about-to-expire token.
      if (token.accessTokenExpires && Date.now() < token.accessTokenExpires - 60_000) {
        return token;
      }
      return refreshAccessToken(token);
    },
    async session({ session, token }) {
      session.accessToken = token.accessToken;
      session.idToken = token.idToken;
      session.roles = token.roles ?? [];
      session.organizationId = token.organizationId ?? null;
      session.investorId = token.investorId ?? null;
      session.error = token.error;
      if (session.user && token.userId) session.user.id = token.userId;
      return session;
    },
  },
});
