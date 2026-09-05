package com.rwashift.platform.units.reporting.api.dto;

import java.math.BigDecimal;
import java.math.BigInteger;

public record InvestorSummaryResponse(
        String investorId,
        BigDecimal totalInvested,
        long investmentCount,
        BigInteger totalUnitsOwned,
        long distinctOfferingsHeld
) {
}
