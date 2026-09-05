package com.rwashift.platform.units.investment.application.port;

import java.util.Optional;

/** Explicit cross-unit contract for units that need an investment's non-confidential shape (Activity). */
public interface InvestmentLookupPort {

    Optional<InvestmentSnapshot> findSnapshot(String investmentId);
}
