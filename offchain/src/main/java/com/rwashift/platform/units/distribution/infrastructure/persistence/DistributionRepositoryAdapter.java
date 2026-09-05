package com.rwashift.platform.units.distribution.infrastructure.persistence;

import com.rwashift.platform.units.distribution.domain.model.Distribution;
import com.rwashift.platform.units.distribution.domain.repository.DistributionRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class DistributionRepositoryAdapter implements DistributionRepository {

    private final DistributionJpaRepository jpaRepository;

    DistributionRepositoryAdapter(DistributionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Distribution save(Distribution distribution) {
        return jpaRepository.save(distribution);
    }

    @Override
    public Optional<Distribution> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Distribution> findByOrganizationId(String organizationId) {
        return jpaRepository.findByOrganizationId(organizationId);
    }

    @Override
    public List<Distribution> findByOfferingId(String offeringId) {
        return jpaRepository.findByOfferingId(offeringId);
    }
}
