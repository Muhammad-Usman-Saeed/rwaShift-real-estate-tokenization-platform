package com.rwashift.platform.units.ownership.application.port;

import java.util.List;

/** Explicit cross-unit contract consumed by Distribution to compute entitlements. */
public interface OwnershipLookupPort {

    List<OwnershipSnapshot> findByOffering(String offeringId);
}
