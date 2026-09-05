/**
 * These two calls don't go through `apiRequest` (client.ts) like every other capability client:
 * the nonce endpoint is public but lives at a fixed backend origin rather than the app's usual
 * API base, and `/login/wallet` isn't a `/api/v1/**` REST resource at all — it's a Spring Security
 * login endpoint that establishes a session cookie (`credentials: "include"`), which is what lets
 * the subsequent `signIn("rwashift")` call skip straight past the password form. See
 * `WalletSignInButton` for the full flow and `offchain`'s `WalletLoginFilter`/`ResourceServerConfig`
 * for the backend side.
 */
const AUTH_ORIGIN = new URL(process.env.NEXT_PUBLIC_API_BASE_URL as string).origin;

export const walletAuthApi = {
  requestSignInMessage: async (address: string): Promise<string> => {
    const response = await fetch(`${AUTH_ORIGIN}/api/v1/public/auth/wallet/nonce?address=${encodeURIComponent(address)}`);
    if (!response.ok) {
      throw new Error("Could not start wallet sign-in");
    }
    const body = (await response.json()) as { message: string };
    return body.message;
  },

  verifySignature: async (walletAddress: string, signature: string, message: string): Promise<void> => {
    const response = await fetch(`${AUTH_ORIGIN}/login/wallet`, {
      method: "POST",
      credentials: "include",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ walletAddress, signature, message }),
    });
    if (!response.ok) {
      const body = (await response.json().catch(() => null)) as { error?: string } | null;
      throw new Error(body?.error ?? "Wallet sign-in failed");
    }
  },
};
