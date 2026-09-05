package com.rwashift.platform.units.legalstructure.application.command;

import com.rwashift.platform.units.legalstructure.domain.model.InvestmentInstrumentType;
import com.rwashift.platform.units.legalstructure.domain.model.LegalEntityType;
import com.rwashift.platform.units.legalstructure.domain.model.RelationshipToAsset;

public record UpdateLegalStructureCommand(
        String legalEntityName,
        LegalEntityType entityType,
        String jurisdiction,
        String registrationNumber,
        RelationshipToAsset relationshipToAsset,
        InvestmentInstrumentType investmentInstrumentType
) {
}
