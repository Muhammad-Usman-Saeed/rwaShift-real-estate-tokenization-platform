package com.rwashift.platform.units.investor.api.dto;

import com.rwashift.platform.units.investor.domain.model.Investor;

public record InvestorResponse(
        String id,
        String userId,
        String investorType,
        String displayName,
        String countryCode,
        String primaryWalletAddress,
        String status
) {
    public static InvestorResponse from(Investor investor) {
        return new InvestorResponse(
                investor.getId(), investor.getUserId(), investor.getInvestorType().name(), investor.getDisplayName(),
                investor.getCountryCode(), investor.getPrimaryWalletAddress(), investor.getStatus().name());
    }
}
