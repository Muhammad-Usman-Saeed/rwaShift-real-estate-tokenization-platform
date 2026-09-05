package com.rwashift.platform.units.legalstructure.application.port;

import java.util.Optional;

/** Explicit cross-unit contract used by Offering to validate a legal structure reference. */
public interface LegalStructureLookupPort {

    boolean existsInOrganization(String legalStructureId, String organizationId);

    /** Used by Activity to name a legal structure in the platform-wide news feed. */
    Optional<String> findDisplayName(String legalStructureId);
}
