package com.rwashift.platform.units.kyc.application.port;

/**
 * Abstraction over the actual identity-verification vendor. V1 ships only
 * {@code DemoKycProvider} (a no-op that fabricates a reference id — real verification is done
 * manually by a compliance officer through {@code KycApplicationService}). A production
 * implementation (Sumsub, Onfido, ...) would implement this same interface; the Investor domain
 * and KYC state machine never couple to a vendor SDK directly.
 */
public interface KycProvider {

    String initiateVerification(String investorId);
}
