package com.rwashift.platform.units.asset.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateAssetRequest(
        @NotBlank @Size(max = 300) String name,
        @Size(max = 4000) String description,
        @NotBlank @Size(max = 500) String location,
        @NotNull @DecimalMin(value = "0.01") BigDecimal valuation,
        @NotBlank @Size(min = 3, max = 3) String currency
) {
}
