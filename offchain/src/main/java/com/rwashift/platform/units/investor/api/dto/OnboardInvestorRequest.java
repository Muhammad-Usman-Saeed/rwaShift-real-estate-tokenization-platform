package com.rwashift.platform.units.investor.api.dto;

import com.rwashift.platform.units.investor.domain.model.InvestorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record OnboardInvestorRequest(
        @NotBlank String userId,
        @NotNull InvestorType investorType,
        @NotBlank @Size(max = 300) String displayName,
        @NotBlank @Size(min = 2, max = 2) String countryCode,
        LocalDate dateOfBirth,
        String entityRegistrationNumber,
        @NotBlank @Pattern(regexp = "^0x[a-fA-F0-9]{40}$") String primaryWalletAddress
) {
}
