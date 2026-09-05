import { http, createConfig } from "wagmi";
import { defineChain } from "viem";
import { sepolia as sepoliaBase } from "viem/chains";
import { injected } from "wagmi/connectors";

/** Not a standard viem chain — defined locally to match Anvil's default chain id (31337). */
export const anvilLocal = defineChain({
  id: 31337,
  name: "Anvil (Local)",
  nativeCurrency: { name: "Ether", symbol: "ETH", decimals: 18 },
  rpcUrls: {
    default: { http: [process.env.NEXT_PUBLIC_ANVIL_RPC_URL ?? "http://127.0.0.1:8545"] },
  },
});

export const sepolia = {
  ...sepoliaBase,
  blockExplorers: {
    default: {
      name: "Etherscan",
      url: process.env.NEXT_PUBLIC_SEPOLIA_EXPLORER_URL ?? "https://sepolia.etherscan.io",
    },
  },
};

/**
 * Minimal EVM wallet integration, per the product spec: a browser-injected wallet (MetaMask or
 * equivalent) only — no WalletConnect/multi-wallet-modal complexity for V1. Two distinct uses of
 * this config: wallet *linking* (`lib/wallet/useLinkedWallet.ts`) is a post-login profile step
 * with no signature involved; wallet *sign-in* (`WalletSignInButton`) is a full OAuth2/OIDC login
 * path in its own right, verified server-side via a signed challenge — see
 * `offchain`'s `WalletAuthenticationProvider`.
 */
export const wagmiConfig = createConfig({
  chains: [sepolia, anvilLocal],
  connectors: [injected()],
  transports: {
    [sepolia.id]: http(process.env.NEXT_PUBLIC_SEPOLIA_RPC_URL),
    [anvilLocal.id]: http(process.env.NEXT_PUBLIC_ANVIL_RPC_URL),
  },
  ssr: true,
});

declare module "wagmi" {
  interface Register {
    config: typeof wagmiConfig;
  }
}
