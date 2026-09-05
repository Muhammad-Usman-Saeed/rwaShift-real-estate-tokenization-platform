package com.rwashift.platform.units.tokenization.application.port;

/**
 * Explicit cross-unit contract: Investment calls this once payment is confirmed, to request
 * that Tokenization mint the settled units on-chain. Tokenization submits the blockchain
 * transaction asynchronously and, on confirmation, calls back into Investment's
 * {@code TokenIssuanceCallbackPort} — submission is never treated as business completion (see
 * README's blockchain-transaction-lifecycle notes).
 */
public interface TokenIssuancePort {

    void requestUnitIssuance(String investmentId, String offeringId, String investorId, long units);
}
