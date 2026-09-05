import { apiRequest } from "@/lib/api/client";
import type { InvestorResponse, InvestorType } from "@/lib/api/types";

export interface OnboardInvestorPayload {
  userId: string;
  investorType: InvestorType;
  displayName: string;
  countryCode: string;
  dateOfBirth?: string;
  entityRegistrationNumber?: string;
  primaryWalletAddress: string;
}

export const investorsApi = {
  onboard: (payload: OnboardInvestorPayload, token?: string) =>
    apiRequest<InvestorResponse>("/investors", { method: "POST", body: payload, token }),
  get: (id: string, token?: string) => apiRequest<InvestorResponse>(`/investors/${id}`, { token }),
  me: (token?: string) => apiRequest<InvestorResponse>("/investors/me", { token }),
  linkWallet: (walletAddress: string, token?: string) =>
    apiRequest<InvestorResponse>("/investors/me/wallet", { method: "POST", body: { walletAddress }, token }),
  /** Admin/compliance directory. */
  list: (token?: string) => apiRequest<InvestorResponse[]>("/investors", { token }),
};
