"use client";

import { useAccount, useConnect, useDisconnect } from "wagmi";

/**
 * Bridges wagmi's connection state with the investor's `primaryWalletAddress` on file (from
 * `investorsApi.me`). Connecting a wallet here only ever populates a form field for the platform
 * onboarding API to store — deliberately no signature step, since this hook is for an
 * *already-authenticated* user updating their profile, not for proving identity. Signature
 * verification does exist elsewhere now (see `WalletSignInButton` + `WalletAuthenticationProvider`
 * on the backend) for the actual sign-in case, where proving wallet ownership is the whole point.
 */
export function useLinkedWallet(linkedAddress?: string | null) {
  const { address, isConnected, connector } = useAccount();
  const { connectors, connect, isPending: isConnecting, error: connectError } = useConnect();
  const { disconnect } = useDisconnect();

  const matchesLinkedAddress =
    !!address && !!linkedAddress && address.toLowerCase() === linkedAddress.toLowerCase();

  return {
    address,
    isConnected,
    connectorName: connector?.name,
    matchesLinkedAddress,
    availableConnectors: connectors,
    connect: (connectorId?: string) => {
      const target = connectorId ? connectors.find((c) => c.id === connectorId) : connectors[0];
      if (target) connect({ connector: target });
    },
    isConnecting,
    connectError,
    disconnect,
  };
}
