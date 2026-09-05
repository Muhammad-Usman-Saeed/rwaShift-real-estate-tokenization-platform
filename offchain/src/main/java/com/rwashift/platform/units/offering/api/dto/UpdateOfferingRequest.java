package com.rwashift.platform.units.offering.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record UpdateOfferingRequest(
        @NotBlank @Size(max = 300) String name,
        @NotNull @DecimalMin(value = "0.01") BigDecimal targetRaise,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @Min(1) long totalUnits,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal unitPrice,
        @NotNull @DecimalMin(value = "0.01") BigDecimal minimumInvestment,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal offeredInterestPercentage,
        Instant openingDate,
        Instant closingDate
) {
}
