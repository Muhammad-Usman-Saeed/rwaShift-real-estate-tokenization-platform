package com.rwashift.platform.units.distribution.infrastructure.persistence;

import com.rwashift.platform.units.distribution.domain.model.DistributionEntitlement;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface DistributionEntitlementJpaRepository extends JpaRepository<DistributionEntitlement, String> {

    List<DistributionEntitlement> findByDistributionId(String distributionId);
}
