package com.rwashift.platform.units.investment.application.command;

import com.rwashift.platform.units.investment.domain.model.PaymentMethod;
import java.math.BigDecimal;

public record InitiateInvestmentCommand(
        String investorId,
        String offeringId,
        BigDecimal amount,
        String idempotencyKey,
        PaymentMethod paymentMethod
) {
}
