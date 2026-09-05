"use client";

import { WagmiProvider } from "wagmi";
import type { ReactNode } from "react";
import { wagmiConfig } from "@/lib/wallet/wagmi-config";

export function WalletProvider({ children }: { children: ReactNode }) {
  return <WagmiProvider config={wagmiConfig}>{children}</WagmiProvider>;
}
