package com.rwashift.platform.units.organization.infrastructure.persistence;

import com.rwashift.platform.units.organization.domain.model.Organization;
import com.rwashift.platform.units.organization.domain.repository.OrganizationRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class OrganizationRepositoryAdapter implements OrganizationRepository {

    private final OrganizationJpaRepository jpaRepository;

    OrganizationRepositoryAdapter(OrganizationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Organization save(Organization organization) {
        return jpaRepository.save(organization);
    }

    @Override
    public Optional<Organization> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Organization> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public boolean existsById(String id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.deleteById(id);
    }
}
