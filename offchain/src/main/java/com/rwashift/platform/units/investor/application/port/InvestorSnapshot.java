package com.rwashift.platform.units.investor.application.port;

public record InvestorSnapshot(
        String investorId,
        String userId,
        String displayName,
        String countryCode,
        String primaryWalletAddress,
        boolean active
) {
}
