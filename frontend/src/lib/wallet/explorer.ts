/** Anvil has no public explorer — only Sepolia links are ever produced. */
export function explorerTxUrl(network: string, txHash: string | null | undefined): string | null {
  if (!txHash || network.toLowerCase() !== "sepolia") return null;
  const base = process.env.NEXT_PUBLIC_SEPOLIA_EXPLORER_URL ?? "https://sepolia.etherscan.io";
  return `${base}/tx/${txHash}`;
}

export function explorerAddressUrl(network: string, address: string | null | undefined): string | null {
  if (!address || network.toLowerCase() !== "sepolia") return null;
  const base = process.env.NEXT_PUBLIC_SEPOLIA_EXPLORER_URL ?? "https://sepolia.etherscan.io";
  return `${base}/address/${address}`;
}
