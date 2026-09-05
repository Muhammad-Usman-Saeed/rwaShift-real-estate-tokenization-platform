package com.rwashift.platform.units.offering.application.port;

import java.util.Optional;

/** Explicit cross-unit contract consumed by Investment/Compliance. */
public interface OfferingLookupPort {

    Optional<OfferingSnapshot> findSnapshot(String offeringId);

    /** Called once a token-issuance blockchain transaction confirms (see Investment/Tokenization). */
    void recordUnitsIssued(String offeringId, long units);

    /**
     * Called by Tokenization when it starts deploying an offering's on-chain token suite —
     * transitions {@code APPROVED -> TOKENIZING} so the later async {@code onTokenizationConfirmed}
     * callback's {@code TOKENIZING -> OPEN} transition is valid when it fires.
     */
    void beginTokenization(String offeringId);
}
