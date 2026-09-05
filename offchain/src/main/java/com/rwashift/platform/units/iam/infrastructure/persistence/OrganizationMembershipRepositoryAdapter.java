package com.rwashift.platform.units.iam.infrastructure.persistence;

import com.rwashift.platform.units.iam.domain.model.OrganizationMembership;
import com.rwashift.platform.units.iam.domain.repository.OrganizationMembershipRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class OrganizationMembershipRepositoryAdapter implements OrganizationMembershipRepository {

    private final OrganizationMembershipJpaRepository jpaRepository;

    OrganizationMembershipRepositoryAdapter(OrganizationMembershipJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public OrganizationMembership save(OrganizationMembership membership) {
        return jpaRepository.save(membership);
    }

    @Override
    public List<OrganizationMembership> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId);
    }

    @Override
    public List<OrganizationMembership> findByOrganizationId(String organizationId) {
        return jpaRepository.findByOrganizationId(organizationId);
    }

    @Override
    public Optional<OrganizationMembership> findByUserIdAndOrganizationId(String userId, String organizationId) {
        return jpaRepository.findByUserIdAndOrganizationId(userId, organizationId);
    }
}
