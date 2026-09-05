package com.rwashift.platform.units.investment.application.port;

/**
 * Explicit cross-unit contract: Tokenization calls this once the on-chain mint transaction
 * requested via {@code TokenIssuancePort} reaches a terminal state (confirmed or failed).
 * Investment owns what "settled" means for its own aggregate; Tokenization never mutates
 * Investment state directly.
 */
public interface TokenIssuanceCallbackPort {

    void onTokenIssuanceConfirmed(String investmentId);

    void onTokenIssuanceFailed(String investmentId, String reason);
}
