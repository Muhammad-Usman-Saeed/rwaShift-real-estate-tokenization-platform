"use client";

import { Button } from "@/components/ui/Button";
import { Wallet } from "@/components/ui/icons";
import { useLinkedWallet } from "@/lib/wallet/useLinkedWallet";
import { truncateHex } from "@/lib/utils/format";

export function ConnectWalletButton({ linkedAddress }: { linkedAddress?: string | null }) {
  const { address, isConnected, connect, disconnect, isConnecting } = useLinkedWallet(linkedAddress);

  if (isConnected && address) {
    return (
      <Button variant="secondary" size="sm" onClick={() => disconnect()}>
        <Wallet className="h-4 w-4" />
        {truncateHex(address)}
      </Button>
    );
  }

  return (
    <Button variant="secondary" size="sm" onClick={() => connect()} isLoading={isConnecting}>
      <Wallet className="h-4 w-4" />
      Connect Wallet
    </Button>
  );
}
