package com.rwashift.platform.units.distribution.api.dto;

import com.rwashift.platform.units.distribution.domain.model.DistributionEntitlement;
import java.math.BigDecimal;
import java.math.BigInteger;

public record DistributionEntitlementResponse(
        String investorId,
        String walletAddress,
        BigInteger unitsAtRecordDate,
        BigDecimal entitlementAmount
) {
    public static DistributionEntitlementResponse from(DistributionEntitlement e) {
        return new DistributionEntitlementResponse(e.getInvestorId(), e.getWalletAddress(), e.getUnitsAtRecordDate(), e.getEntitlementAmount());
    }
}
