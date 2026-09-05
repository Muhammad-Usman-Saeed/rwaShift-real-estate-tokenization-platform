package com.rwashift.platform.units.distribution.application.port;

import java.util.Optional;

/** Explicit cross-unit contract for units that need to resolve a distribution back to its offering (Activity). */
public interface DistributionLookupPort {

    Optional<String> findOfferingId(String distributionId);
}
