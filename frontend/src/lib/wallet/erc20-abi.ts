/**
 * Minimal read-only ABI fragment for the T-REX `Token` contract's ERC-20-compatible surface (see
 * `onchain/README.md#t-rex-token-surface-used-directly`). The frontend never writes to this
 * contract directly — all mint/transfer/freeze operations are agent-gated and performed by the
 * backend's Tokenization unit. This ABI exists only so the Portfolio/Ownership screens can read a
 * live on-chain balance next to the backend's own ownership projection.
 */
export const erc20ReadAbi = [
  {
    type: "function",
    name: "balanceOf",
    stateMutability: "view",
    inputs: [{ name: "account", type: "address" }],
    outputs: [{ name: "", type: "uint256" }],
  },
  {
    type: "function",
    name: "decimals",
    stateMutability: "view",
    inputs: [],
    outputs: [{ name: "", type: "uint8" }],
  },
  {
    type: "function",
    name: "symbol",
    stateMutability: "view",
    inputs: [],
    outputs: [{ name: "", type: "string" }],
  },
] as const;
