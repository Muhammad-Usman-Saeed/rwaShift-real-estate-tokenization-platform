package com.rwashift.platform.units.investment.api.dto;

import com.rwashift.platform.units.investment.domain.model.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record InitiateInvestmentRequest(
        @NotBlank String offeringId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotNull PaymentMethod paymentMethod
) {
}
