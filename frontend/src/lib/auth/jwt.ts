/**
 * Decodes (never verifies) a JWT payload. Safe here specifically because this only ever runs on
 * tokens fetched directly from our own configured `AUTH_ISSUER` token endpoint over TLS, inside
 * the Auth.js server-side `jwt` callback — the backend resource server independently verifies the
 * signature on every API call, so this decode is purely to read `roles`/`org_id`/`investor_id`
 * for the session, not an authorization decision in itself.
 */
export interface AccessTokenClaims {
  sub?: string;
  email?: string;
  roles?: string[];
  org_id?: string | null;
  investor_id?: string | null;
  exp?: number;
}

export function decodeJwtPayload<T = AccessTokenClaims>(jwt: string): T {
  const payload = jwt.split(".")[1];
  if (!payload) throw new Error("Malformed JWT");
  const json = Buffer.from(payload, "base64url").toString("utf-8");
  return JSON.parse(json) as T;
}
