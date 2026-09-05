import type { DefaultSession } from "next-auth";

declare module "next-auth" {
  interface Session {
    accessToken?: string;
    /** ID token from the initial login — only used server-side to pass as `id_token_hint` on RP-initiated logout (see `lib/auth/actions.ts`). */
    idToken?: string;
    roles: string[];
    organizationId: string | null;
    investorId: string | null;
    error?: "RefreshAccessTokenError";
    user: DefaultSession["user"] & { id?: string };
  }
}

declare module "next-auth/jwt" {
  interface JWT {
    accessToken?: string;
    refreshToken?: string;
    idToken?: string;
    accessTokenExpires?: number;
    roles?: string[];
    organizationId?: string | null;
    investorId?: string | null;
    userId?: string;
    error?: "RefreshAccessTokenError";
  }
}
