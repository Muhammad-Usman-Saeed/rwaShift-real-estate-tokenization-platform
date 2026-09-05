package com.rwashift.platform.units.legalstructure.api.dto;

import com.rwashift.platform.units.legalstructure.domain.model.InvestmentInstrumentType;
import com.rwashift.platform.units.legalstructure.domain.model.LegalEntityType;
import com.rwashift.platform.units.legalstructure.domain.model.RelationshipToAsset;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLegalStructureRequest(
        @NotBlank String assetId,
        @NotBlank @Size(max = 300) String legalEntityName,
        @NotNull LegalEntityType entityType,
        @NotBlank @Size(max = 100) String jurisdiction,
        @Size(max = 100) String registrationNumber,
        @NotNull RelationshipToAsset relationshipToAsset,
        @NotNull InvestmentInstrumentType investmentInstrumentType
) {
}
