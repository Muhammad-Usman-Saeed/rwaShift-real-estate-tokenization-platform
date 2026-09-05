import { apiRequest } from "@/lib/api/client";
import type { BlockchainTransactionResponse, ContractDeploymentResponse, OrganizationWalletResponse } from "@/lib/api/types";

export const tokenizationApi = {
  deploy: (offeringId: string, symbol: string, token?: string) =>
    apiRequest<void>(`/tokenization/offerings/${offeringId}/deploy`, { method: "POST", body: { symbol }, token }),
  deployments: (offeringId: string, token?: string) =>
    apiRequest<ContractDeploymentResponse[]>(`/tokenization/offerings/${offeringId}/deployments`, { token }),
  /** Admin-wide, across all offerings. */
  allDeployments: (token?: string) => apiRequest<ContractDeploymentResponse[]>("/tokenization/deployments", { token }),
  /** Admin-wide blockchain transaction ledger. */
  transactions: (token?: string) => apiRequest<BlockchainTransactionResponse[]>("/tokenization/transactions", { token }),
  /** Every organization's on-chain wallet + live gas balance — who needs funding. */
  organizationWallets: (token?: string) =>
    apiRequest<OrganizationWalletResponse[]>("/tokenization/organization-wallets", { token }),
  /** Resubmits a FAILED transaction — today, only the offering-tokenization "unpause" step. */
  retryTransaction: (transactionId: string, token?: string) =>
    apiRequest<void>(`/tokenization/transactions/${transactionId}/retry`, { method: "POST", token }),
};
