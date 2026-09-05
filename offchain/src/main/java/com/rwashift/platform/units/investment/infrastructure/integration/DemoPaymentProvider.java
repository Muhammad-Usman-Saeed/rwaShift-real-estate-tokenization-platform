package com.rwashift.platform.units.investment.infrastructure.integration;

import com.rwashift.platform.units.investment.application.port.PaymentProvider;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** V1 demo implementation: real settlement is simulated by an explicit admin confirmation call. */
@Component
class DemoPaymentProvider implements PaymentProvider {

    @Override
    public PaymentInitiationResult initiatePayment(String investmentId, BigDecimal amount, String currency) {
        return new PaymentInitiationResult("demo-payment-" + UUID.randomUUID());
    }
}
