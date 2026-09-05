"use client";

import { useReadContract } from "wagmi";
import { erc20ReadAbi } from "@/lib/wallet/erc20-abi";
import type { Address } from "viem";

export function useTokenBalance(tokenAddress?: string, walletAddress?: string, chainId?: number) {
  const enabled = Boolean(tokenAddress && walletAddress);

  const balance = useReadContract({
    address: tokenAddress as Address | undefined,
    abi: erc20ReadAbi,
    functionName: "balanceOf",
    args: walletAddress ? [walletAddress as Address] : undefined,
    chainId: chainId as 31337 | 11155111 | undefined,
    query: { enabled, staleTime: 15_000 },
  });

  const symbol = useReadContract({
    address: tokenAddress as Address | undefined,
    abi: erc20ReadAbi,
    functionName: "symbol",
    chainId: chainId as 31337 | 11155111 | undefined,
    query: { enabled, staleTime: 60_000 },
  });

  return {
    balance: balance.data,
    symbol: symbol.data,
    isLoading: balance.isLoading || symbol.isLoading,
    isError: balance.isError,
    refetch: balance.refetch,
  };
}
