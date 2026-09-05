package com.rwashift.platform.units.distribution.infrastructure.persistence;

import com.rwashift.platform.units.distribution.domain.model.DistributionEntitlement;
import com.rwashift.platform.units.distribution.domain.repository.DistributionEntitlementRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
class DistributionEntitlementRepositoryAdapter implements DistributionEntitlementRepository {

    private final DistributionEntitlementJpaRepository jpaRepository;

    DistributionEntitlementRepositoryAdapter(DistributionEntitlementJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<DistributionEntitlement> saveAll(List<DistributionEntitlement> entitlements) {
        return jpaRepository.saveAll(entitlements);
    }

    @Override
    public List<DistributionEntitlement> findByDistributionId(String distributionId) {
        return jpaRepository.findByDistributionId(distributionId);
    }
}
