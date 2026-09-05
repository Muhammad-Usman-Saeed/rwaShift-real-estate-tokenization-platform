package com.rwashift.platform.units.offering.application.command;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateOfferingCommand(
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
        Instant closingDate
) {
}
