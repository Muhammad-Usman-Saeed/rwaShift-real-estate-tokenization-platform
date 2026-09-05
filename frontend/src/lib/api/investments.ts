import { apiRequest } from "@/lib/api/client";
import type { InvestmentResponse } from "@/lib/api/types";

export type PaymentMethod = "BANK_TRANSFER" | "CRYPTO_WALLET";

export interface InitiateInvestmentPayload {
  offeringId: string;
  amount: number;
  paymentMethod: PaymentMethod;
}

export interface CryptoPaymentInstructionsResponse {
  investmentId: string;
  paymentRouterAddress: string;
  usdcTokenAddress: string;
  amountUsdc: number;
  investorWalletAddress: string | null;
}

export const investmentsApi = {
  initiate: (payload: InitiateInvestmentPayload, idempotencyKey: string, token?: string) =>
    apiRequest<InvestmentResponse>("/investments", {
      method: "POST",
      body: payload,
      token,
      headers: { "Idempotency-Key": idempotencyKey },
    }),
  recheckEligibility: (investmentId: string, token?: string) =>
    apiRequest<InvestmentResponse>(`/investments/${investmentId}/recheck-eligibility`, { method: "POST", token }),
  /** Demo settlement confirmation for BANK_TRANSFER only (see product spec section 16 — simulated, not a real bank transfer). */
  confirmPayment: (investmentId: string, token?: string) =>
    apiRequest<InvestmentResponse>(`/investments/${investmentId}/confirm-payment`, { method: "POST", token }),
  /** CRYPTO_WALLET only — deposit address + expected amount; confirmation happens automatically once the transfer is detected on-chain. */
  cryptoPaymentInstructions: (investmentId: string, token?: string) =>
    apiRequest<CryptoPaymentInstructionsResponse>(`/investments/${investmentId}/crypto-payment-instructions`, { token }),
  /** Investor's own self-service withdrawal — only valid before payment is confirmed (ELIGIBILITY_PENDING/PAYMENT_PENDING). */
  cancel: (investmentId: string, token?: string) =>
    apiRequest<InvestmentResponse>(`/investments/${investmentId}/cancel`, { method: "POST", token }),
  get: (investmentId: string, token?: string) => apiRequest<InvestmentResponse>(`/investments/${investmentId}`, { token }),
  mine: (token?: string) => apiRequest<InvestmentResponse[]>("/investments/mine", { token }),
  forOrganization: (token?: string) => apiRequest<InvestmentResponse[]>("/investments", { token }),
};
