package com.rwashift.platform.units.reporting.api.dto;

import java.math.BigDecimal;

public record IssuerSummaryResponse(
        String organizationId,
        long totalAssets,
        BigDecimal totalAssetValuation,
        long activeOfferings,
        BigDecimal totalCapitalRaised,
        long distinctInvestorCount,
        long totalUnitsIssued,
        long totalUnitsAvailable,
        long distributionCount
) {
}
