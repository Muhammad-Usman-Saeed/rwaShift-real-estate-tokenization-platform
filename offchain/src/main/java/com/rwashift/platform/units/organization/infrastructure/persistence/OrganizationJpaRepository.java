package com.rwashift.platform.units.organization.infrastructure.persistence;

import com.rwashift.platform.units.organization.domain.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrganizationJpaRepository extends JpaRepository<Organization, String> {
}
