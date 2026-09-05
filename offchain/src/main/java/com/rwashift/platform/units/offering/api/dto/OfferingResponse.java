package com.rwashift.platform.units.offering.api.dto;

import com.rwashift.platform.units.offering.domain.model.Offering;
import java.math.BigDecimal;
import java.time.Instant;

public record OfferingResponse(
        String id,
        String organizationId,
        String assetId,
        String legalStructureId,
        String name,
        BigDecimal targetRaise,
        String currency,
        long totalUnits,
        BigDecimal unitPrice,
        BigDecimal minimumInvestment,
        BigDecimal offeredInterestPercentage,
        Instant openingDate,
        Instant closingDate,
        String status,
        long unitsIssued,
        long unitsAvailable
) {
    public static OfferingResponse from(Offering offering) {
        return new OfferingResponse(
                offering.getId(), offering.getOrganizationId(), offering.getAssetId(), offering.getLegalStructureId(),
                offering.getName(), offering.getTargetRaise(), offering.getCurrency(), offering.getTotalUnits(),
                offering.getUnitPrice(), offering.getMinimumInvestment(), offering.getOfferedInterestPercentage(),
                offering.getOpeningDate(), offering.getClosingDate(), offering.getStatus().name(),
                offering.getUnitsIssued(), offering.getUnitsAvailable());
    }
}
