import { CHAINS } from "@/lib/utils/constants";

const chainKey = (process.env.NEXT_PUBLIC_CHAIN ?? "sepolia") as keyof typeof CHAINS;

export const NEXT_PUBLIC_CHAIN_LABEL = CHAINS[chainKey]?.name ?? "Sepolia";
export const NEXT_PUBLIC_CHAIN_ID = CHAINS[chainKey]?.id ?? CHAINS.sepolia.id;
