package com.rwashift.platform.units.investment.domain.model;

/**
 * How the investor intends to settle this investment. {@code BANK_TRANSFER} keeps today's V1
 * behavior (an admin manually confirms via {@code InvestmentApplicationService.confirmPayment}
 * once a wire lands — see {@code PaymentProvider}'s Javadoc). {@code CRYPTO_WALLET} is settled
 * automatically once {@code CryptoPaymentWatcher} observes a matching USDC transfer from the
 * investor's registered wallet to the platform's payment-collection address — no admin step.
 */
public enum PaymentMethod {
    BANK_TRANSFER,
    CRYPTO_WALLET
}
