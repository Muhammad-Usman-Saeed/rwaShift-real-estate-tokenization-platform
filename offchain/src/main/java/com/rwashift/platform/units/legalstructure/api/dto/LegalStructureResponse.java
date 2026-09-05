package com.rwashift.platform.units.legalstructure.api.dto;

import com.rwashift.platform.units.legalstructure.domain.model.LegalStructure;

public record LegalStructureResponse(
        String id,
        String organizationId,
        String assetId,
        String legalEntityName,
        String entityType,
        String jurisdiction,
        String registrationNumber,
        String relationshipToAsset,
        String investmentInstrumentType,
        String status
) {
    public static LegalStructureResponse from(LegalStructure legalStructure) {
        return new LegalStructureResponse(
                legalStructure.getId(),
                legalStructure.getOrganizationId(),
                legalStructure.getAssetId(),
                legalStructure.getLegalEntityName(),
                legalStructure.getEntityType().name(),
                legalStructure.getJurisdiction(),
                legalStructure.getRegistrationNumber(),
                legalStructure.getRelationshipToAsset().name(),
                legalStructure.getInvestmentInstrumentType().name(),
                legalStructure.getStatus().name());
    }
}
