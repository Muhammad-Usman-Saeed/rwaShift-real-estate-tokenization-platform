package com.rwashift.platform.units.tokenization.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DeployOfferingTokenRequest(
        @NotBlank @Pattern(regexp = "^[A-Z0-9]{2,10}$") String symbol
) {
}
