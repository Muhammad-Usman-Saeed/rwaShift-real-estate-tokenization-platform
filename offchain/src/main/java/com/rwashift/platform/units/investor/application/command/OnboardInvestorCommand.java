package com.rwashift.platform.units.investor.application.command;

import com.rwashift.platform.units.investor.domain.model.InvestorType;
import java.time.LocalDate;

public record OnboardInvestorCommand(
        String userId,
        InvestorType investorType,
        String displayName,
        String countryCode,
        LocalDate dateOfBirth,
        String entityRegistrationNumber,
        String primaryWalletAddress
) {
}
