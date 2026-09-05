import { apiRequest } from "@/lib/api/client";

export interface SignUpInvestorPayload {
  email: string;
  password: string;
  displayName: string;
}

export interface SignUpInvestorResponse {
  userId: string;
  email: string;
  displayName: string;
}

export const investorSignUpApi = {
  signUp: (payload: SignUpInvestorPayload) =>
    apiRequest<SignUpInvestorResponse>("/public/investors/signup", { method: "POST", body: payload }),
};
