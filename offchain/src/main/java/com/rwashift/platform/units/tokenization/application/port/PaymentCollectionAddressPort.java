package com.rwashift.platform.units.tokenization.application.port;

/**
 * Lets Investment build crypto-payment instructions (deposit address + expected token) without
 * depending on {@code BlockchainProperties} or any other tokenization-owned config directly —
 * same "explicit cross-unit port" pattern as {@code OfferingLookupPort}/{@code InvestorLookupPort}.
 */
public interface PaymentCollectionAddressPort {

    /** The wallet investors send USDC to. Never null once the platform is deployed — see onchain/README. */
    String getPaymentCollectionAddress();

    /** The USDC (or USDC-equivalent) ERC20 contract address on the platform's configured network. */
    String getUsdcTokenAddress();

    /**
     * The {@code RwaShiftPaymentRouter} contract investors actually pay through — see
     * onchain/src/core/RwaShiftPaymentRouter.sol. Investors {@code approve} this address for USDC,
     * then call its {@code payInvestment(investmentId, amount)}, which tags the payment with the
     * investment id on-chain (in the {@code PaymentReceived} event) so {@code CryptoPaymentWatcher}
     * never has to guess which investment a payment was for.
     */
    String getPaymentRouterAddress();
}
