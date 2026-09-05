package com.rwashift.platform.units.iam.infrastructure.persistence;

import com.rwashift.platform.units.iam.domain.model.OrganizationMembership;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrganizationMembershipJpaRepository extends JpaRepository<OrganizationMembership, String> {

    List<OrganizationMembership> findByUserId(String userId);

    List<OrganizationMembership> findByOrganizationId(String organizationId);

    Optional<OrganizationMembership> findByUserIdAndOrganizationId(String userId, String organizationId);
}
