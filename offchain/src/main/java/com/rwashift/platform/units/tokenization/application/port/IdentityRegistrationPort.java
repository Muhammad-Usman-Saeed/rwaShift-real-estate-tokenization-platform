package com.rwashift.platform.units.tokenization.application.port;

/**
 * Explicit cross-unit contract: Compliance calls this the moment an investor becomes eligible
 * for an offering, so Tokenization can register the investor's wallet in the offering's
 * on-chain {@code IdentityRegistry} (via {@code RwaShiftIdentityGateway} on-chain — see the
 * `onchain/` project). This is the ONLY way any other unit ever triggers a blockchain write;
 * Compliance itself never touches Web3j.
 */
public interface IdentityRegistrationPort {

    void registerInvestorIdentity(String investorId, String offeringId);
}
