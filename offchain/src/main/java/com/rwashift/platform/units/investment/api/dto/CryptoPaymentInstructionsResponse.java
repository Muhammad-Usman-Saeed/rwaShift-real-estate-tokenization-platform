package com.rwashift.platform.units.investment.api.dto;

import java.math.BigDecimal;

/**
 * Everything the frontend needs to pay through {@code RwaShiftPaymentRouter} for a CRYPTO_WALLET
 * investment: {@code approve} {@code usdcTokenAddress} for {@code amountUsdc} to
 * {@code paymentRouterAddress}, then call the router's {@code payInvestment(investmentId, amount)}.
 * {@code amountUsdc} is the same value as the investment's {@code amount}, just spelled out
 * separately for display clarity (1 USD == 1 USDC in this demo's exchange model — see
 * {@code InvestmentApplicationService#getCryptoPaymentInstructions}).
 */
public record CryptoPaymentInstructionsResponse(
        String investmentId,
        String paymentRouterAddress,
        String usdcTokenAddress,
        BigDecimal amountUsdc,
        String investorWalletAddress) {
}
