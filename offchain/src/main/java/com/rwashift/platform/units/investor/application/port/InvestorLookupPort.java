package com.rwashift.platform.units.investor.application.port;

import java.util.Optional;

/** Explicit cross-unit contract consumed by Compliance, Investment, and Tokenization. */
public interface InvestorLookupPort {

    Optional<InvestorSnapshot> findSnapshot(String investorId);

    Optional<InvestorSnapshot> findByWalletAddress(String walletAddress);
}
