package com.rwashift.platform.units.legalstructure.domain.repository;

import com.rwashift.platform.units.legalstructure.domain.model.LegalStructure;
import java.util.List;
import java.util.Optional;

public interface LegalStructureRepository {

    LegalStructure save(LegalStructure legalStructure);

    Optional<LegalStructure> findById(String id);

    List<LegalStructure> findByOrganizationId(String organizationId);

    List<LegalStructure> findAll();

    boolean existsByIdAndOrganizationId(String id, String organizationId);
}
