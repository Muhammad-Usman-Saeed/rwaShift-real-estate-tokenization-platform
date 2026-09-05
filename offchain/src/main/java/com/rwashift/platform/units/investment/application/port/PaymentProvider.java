package com.rwashift.platform.units.investment.application.port;

import java.math.BigDecimal;

/**
 * Abstraction over the real banking/payment rail. V1 ships only {@code DemoPaymentProvider}
 * (payment confirmation is manually simulated by a platform admin through
 * {@code InvestmentApplicationService.confirmPayment}). A production payment provider
 * (Stripe, a banking partner API, ...) implements this same interface — Investment's aggregate
 * and state machine do not change when that happens.
 */
public interface PaymentProvider {

    PaymentInitiationResult initiatePayment(String investmentId, BigDecimal amount, String currency);

    record PaymentInitiationResult(String paymentReference) {
    }
}
