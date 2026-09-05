"use client";

import { useState } from "react";
import { signIn } from "next-auth/react";
import { useAccount, useConnect, useSignMessage } from "wagmi";
import { Button } from "@/components/ui/Button";
import { Wallet } from "@/components/ui/icons";
import { useToast } from "@/components/ui/Toast";
import { walletAuthApi } from "@/lib/api/wallet-auth";
import { logger } from "@/lib/utils/logger";

/**
 * The full "Sign-In With Ethereum" flow: connect -> request a one-time message from the backend
 * -> sign it in the wallet (off-chain, no gas) -> submit the signature to establish a verified
 * backend session -> hand off to the exact same `signIn("rwashift")` the password button uses.
 * Because that last step finds a session Spring Security already trusts, it skips the password
 * form entirely and returns the same JWT shape either path produces — see
 * `WalletAuthenticationProvider` on the backend for why that's guaranteed rather than coincidental.
 */
export function WalletSignInButton({ callbackUrl }: { callbackUrl?: string }) {
  const [isLoading, setIsLoading] = useState(false);
  const { address, isConnected } = useAccount();
  const { connectors, connectAsync } = useConnect();
  const { signMessageAsync } = useSignMessage();
  const { push } = useToast();

  async function handleClick() {
    setIsLoading(true);
    try {
      let walletAddress = address;
      if (!isConnected || !walletAddress) {
        const connector = connectors[0];
        if (!connector) {
          push({ title: "No wallet found", description: "Install MetaMask or a compatible browser wallet.", variant: "error" });
          return;
        }
        const result = await connectAsync({ connector });
        walletAddress = result.accounts[0];
      }
      if (!walletAddress) {
        throw new Error("No wallet address available after connecting");
      }

      logger.debug("wallet sign-in: requesting message", { walletAddress });
      const message = await walletAuthApi.requestSignInMessage(walletAddress);

      const signature = await signMessageAsync({ account: walletAddress, message });

      logger.debug("wallet sign-in: verifying signature", { walletAddress });
      await walletAuthApi.verifySignature(walletAddress, signature, message);

      logger.info("wallet sign-in: verified, completing OIDC handoff", { walletAddress });
      await signIn("rwashift", { callbackUrl: callbackUrl ?? "/" });
    } catch (cause) {
      const errorMessage = cause instanceof Error ? cause.message : "Wallet sign-in failed";
      logger.error("wallet sign-in failed", { error: errorMessage });
      push({ title: "Could not sign in with wallet", description: errorMessage, variant: "error" });
      setIsLoading(false);
    }
  }

  return (
    <Button size="lg" variant="secondary" className="w-full" isLoading={isLoading} onClick={handleClick}>
      <Wallet className="h-4 w-4" />
      Sign in with Wallet
    </Button>
  );
}
