package com.rwashift.platform.units.investment.application.port;

import java.math.BigDecimal;

/**
 * Called by {@code CryptoPaymentWatcher} (tokenization unit) once it observes a
 * {@code PaymentReceived} event from {@code RwaShiftPaymentRouter} — investors pay through the
 * router's {@code payInvestment(investmentId, amount)}, which tags the payment with the exact
 * investment id it settles, so the watcher reports that id directly rather than inferring it.
 * Investment still re-validates the investment is actually {@code PAYMENT_PENDING}/
 * {@code CRYPTO_WALLET} and that the amount matches before confirming — the id removes the
 * *ambiguity* in matching, it doesn't remove the need to validate.
 */
public interface CryptoPaymentConfirmationPort {

    /** {@code amount} is already converted from USDC's 6-decimal on-chain units to a plain USD-equivalent value. */
    void onCryptoPaymentDetected(String investmentId, String txHash, String payerWalletAddress, BigDecimal amount);
}
