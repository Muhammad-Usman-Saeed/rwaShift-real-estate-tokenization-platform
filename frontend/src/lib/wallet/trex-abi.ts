/**
 * Minimal ABI fragments for the T-REX `Token` and `IdentityRegistry` contracts used by the
 * Controlled Transfer demo (product spec section 20). Every call here is genuine — `transfer` is
 * simulated via `eth_call` before it's ever submitted, so a rejected transfer is a real on-chain
 * revert (T-REX's own `identityRegistry.isVerified`/compliance module checks inside `_transfer`),
 * never a scripted UI state. See `onchain/README.md#compliance` for what these checks actually do.
 */
export const trexTokenAbi = [
  {
    type: "function",
    name: "transfer",
    stateMutability: "nonpayable",
    inputs: [
      { name: "to", type: "address" },
      { name: "amount", type: "uint256" },
    ],
    outputs: [{ name: "", type: "bool" }],
  },
  {
    type: "function",
    name: "identityRegistry",
    stateMutability: "view",
    inputs: [],
    outputs: [{ name: "", type: "address" }],
  },
  {
    type: "function",
    name: "balanceOf",
    stateMutability: "view",
    inputs: [{ name: "account", type: "address" }],
    outputs: [{ name: "", type: "uint256" }],
  },
] as const;

export const identityRegistryAbi = [
  {
    type: "function",
    name: "isVerified",
    stateMutability: "view",
    inputs: [{ name: "_userAddress", type: "address" }],
    outputs: [{ name: "", type: "bool" }],
  },
] as const;

/** Plain ERC20 fragment — `approve` is what the crypto-payment flow uses now (see InvestmentStatusPanel.tsx). */
export const erc20Abi = [
  {
    type: "function",
    name: "transfer",
    stateMutability: "nonpayable",
    inputs: [
      { name: "to", type: "address" },
      { name: "amount", type: "uint256" },
    ],
    outputs: [{ name: "", type: "bool" }],
  },
  {
    type: "function",
    name: "approve",
    stateMutability: "nonpayable",
    inputs: [
      { name: "spender", type: "address" },
      { name: "amount", type: "uint256" },
    ],
    outputs: [{ name: "", type: "bool" }],
  },
  {
    type: "function",
    name: "allowance",
    stateMutability: "view",
    inputs: [
      { name: "owner", type: "address" },
      { name: "spender", type: "address" },
    ],
    outputs: [{ name: "", type: "uint256" }],
  },
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
] as const;

/**
 * `RwaShiftPaymentRouter` — investors pay through this contract's `payInvestment`, not a plain
 * ERC20 `transfer`, so the payment is tagged on-chain with which investment it settles (see
 * onchain/src/core/RwaShiftPaymentRouter.sol and docs/critical-analysis.md #3).
 */
export const paymentRouterAbi = [
  {
    type: "function",
    name: "payInvestment",
    stateMutability: "nonpayable",
    inputs: [
      { name: "investmentId", type: "string" },
      { name: "amount", type: "uint256" },
    ],
    outputs: [],
  },
] as const;
