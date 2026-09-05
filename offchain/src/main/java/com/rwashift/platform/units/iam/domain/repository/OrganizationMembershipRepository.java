package com.rwashift.platform.units.iam.domain.repository;

import com.rwashift.platform.units.iam.domain.model.OrganizationMembership;
import java.util.List;
import java.util.Optional;

public interface OrganizationMembershipRepository {

    OrganizationMembership save(OrganizationMembership membership);

    List<OrganizationMembership> findByUserId(String userId);

    List<OrganizationMembership> findByOrganizationId(String organizationId);

    Optional<OrganizationMembership> findByUserIdAndOrganizationId(String userId, String organizationId);
}
