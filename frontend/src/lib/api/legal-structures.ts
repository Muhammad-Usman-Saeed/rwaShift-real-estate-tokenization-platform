import { apiRequest } from "@/lib/api/client";
import type {
  InvestmentInstrumentType,
  LegalEntityType,
  LegalStructureResponse,
  RelationshipToAsset,
} from "@/lib/api/types";

export interface CreateLegalStructurePayload {
  assetId: string;
  legalEntityName: string;
  entityType: LegalEntityType;
  jurisdiction: string;
  registrationNumber?: string;
  relationshipToAsset: RelationshipToAsset;
  investmentInstrumentType: InvestmentInstrumentType;
}

export type UpdateLegalStructurePayload = Omit<CreateLegalStructurePayload, "assetId">;

export const legalStructuresApi = {
  list: (token?: string) => apiRequest<LegalStructureResponse[]>("/legal-structures", { token }),
  get: (id: string, token?: string) => apiRequest<LegalStructureResponse>(`/legal-structures/${id}`, { token }),
  create: (payload: CreateLegalStructurePayload, token?: string) =>
    apiRequest<LegalStructureResponse>("/legal-structures", { method: "POST", body: payload, token }),
  update: (id: string, payload: UpdateLegalStructurePayload, token?: string) =>
    apiRequest<LegalStructureResponse>(`/legal-structures/${id}`, { method: "PUT", body: payload, token }),
  activate: (id: string, token?: string) =>
    apiRequest<LegalStructureResponse>(`/legal-structures/${id}/activate`, { method: "POST", token }),
};
