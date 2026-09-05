package com.rwashift.platform.units.compliance.application.port;

/** Explicit cross-unit contract consumed by Investment before accepting a new investment. */
public interface EligibilityLookupPort {

    /** Reads whatever eligibility decision was last computed — may be stale or never computed. */
    boolean isEligible(String investorId, String offeringId);

    /**
     * Runs every registered eligibility rule fresh (KYC, investor active/wallet, offering open)
     * and persists the result, returning whether it came out eligible. Investment calls this —
     * never {@link #isEligible} alone — at the moments eligibility actually needs to be current
     * (investment initiation, and the compliance-triggered recheck of an investment stuck at
     * ELIGIBILITY_PENDING), since nothing else in the platform evaluates eligibility on its own.
     */
    boolean evaluate(String investorId, String offeringId);
}
