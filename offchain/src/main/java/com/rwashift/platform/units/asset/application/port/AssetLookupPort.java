package com.rwashift.platform.units.asset.application.port;

import java.util.Optional;

/**
 * Explicit cross-unit contract for units that need to confirm an asset exists within a given
 * organization (LegalStructure, Offering) without depending on the Asset JPA entity or
 * repository directly.
 */
public interface AssetLookupPort {

    boolean existsInOrganization(String assetId, String organizationId);

    /** Used by Organization to block deleting an organization that still has assets attached. */
    boolean hasAnyInOrganization(String organizationId);

    /** Used by Activity to name an asset in the platform-wide news feed rather than saying "an asset." */
    Optional<String> findDisplayName(String assetId);
}
