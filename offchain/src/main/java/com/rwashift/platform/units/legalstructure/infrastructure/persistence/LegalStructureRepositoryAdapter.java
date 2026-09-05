package com.rwashift.platform.units.legalstructure.infrastructure.persistence;

import com.rwashift.platform.units.legalstructure.domain.model.LegalStructure;
import com.rwashift.platform.units.legalstructure.domain.repository.LegalStructureRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class LegalStructureRepositoryAdapter implements LegalStructureRepository {

    private final LegalStructureJpaRepository jpaRepository;

    LegalStructureRepositoryAdapter(LegalStructureJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public LegalStructure save(LegalStructure legalStructure) {
        return jpaRepository.save(legalStructure);
    }

    @Override
    public Optional<LegalStructure> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<LegalStructure> findByOrganizationId(String organizationId) {
        return jpaRepository.findByOrganizationId(organizationId);
    }

    @Override
    public List<LegalStructure> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public boolean existsByIdAndOrganizationId(String id, String organizationId) {
        return jpaRepository.existsByIdAndOrganizationId(id, organizationId);
    }
}
