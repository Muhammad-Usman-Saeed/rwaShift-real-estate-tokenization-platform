package com.rwashift.platform.units.offering.application.port;

/**
 * Explicit cross-unit contract: Tokenization calls this once an offering's on-chain token suite
 * is deployed and confirmed, so Offering can transition {@code TOKENIZING -> OPEN}.
 */
public interface OfferingTokenizationCallbackPort {

    void onTokenizationConfirmed(String offeringId);
}
