package com.rwashift.platform.units.investor.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LinkWalletRequest(
        @NotBlank @Pattern(regexp = "^0x[a-fA-F0-9]{40}$") String walletAddress
) {
}
