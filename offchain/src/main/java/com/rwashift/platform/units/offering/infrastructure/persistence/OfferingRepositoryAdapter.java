package com.rwashift.platform.units.offering.infrastructure.persistence;

import com.rwashift.platform.units.offering.domain.model.Offering;
import com.rwashift.platform.units.offering.domain.repository.OfferingRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class OfferingRepositoryAdapter implements OfferingRepository {

    private final OfferingJpaRepository jpaRepository;

    OfferingRepositoryAdapter(OfferingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Offering save(Offering offering) {
        return jpaRepository.save(offering);
    }

    @Override
    public Optional<Offering> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Offering> findByOrganizationId(String organizationId) {
        return jpaRepository.findByOrganizationId(organizationId);
    }

    @Override
    public List<Offering> findAll() {
        return jpaRepository.findAll();
    }
}
