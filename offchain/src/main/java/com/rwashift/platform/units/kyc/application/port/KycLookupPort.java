package com.rwashift.platform.units.kyc.application.port;

/** Explicit cross-unit contract consumed by Compliance when computing offering eligibility. */
public interface KycLookupPort {

    boolean isVerified(String investorId);
}
