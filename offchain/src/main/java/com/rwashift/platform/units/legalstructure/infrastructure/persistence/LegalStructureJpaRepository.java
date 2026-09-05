package com.rwashift.platform.units.legalstructure.infrastructure.persistence;

import com.rwashift.platform.units.legalstructure.domain.model.LegalStructure;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface LegalStructureJpaRepository extends JpaRepository<LegalStructure, String> {

    List<LegalStructure> findByOrganizationId(String organizationId);

    boolean existsByIdAndOrganizationId(String id, String organizationId);
}
