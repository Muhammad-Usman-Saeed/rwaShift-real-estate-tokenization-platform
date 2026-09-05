package com.rwashift.platform.units.distribution.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;

public record CreateDistributionRequest(
        @NotBlank String offeringId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal totalAmount,
        @NotBlank String currency,
        @NotNull Instant recordDate
) {
}
