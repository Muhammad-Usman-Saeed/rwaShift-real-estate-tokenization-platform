package com.rwashift.platform.units.organization.application.port;

import java.util.Optional;

/** Explicit cross-unit contract for units that need an organization's display name (Activity). */
public interface OrganizationLookupPort {

    Optional<String> findDisplayName(String organizationId);
}
