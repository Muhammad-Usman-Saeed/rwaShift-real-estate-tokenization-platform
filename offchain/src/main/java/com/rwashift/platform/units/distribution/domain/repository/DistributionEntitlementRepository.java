package com.rwashift.platform.units.distribution.domain.repository;

import com.rwashift.platform.units.distribution.domain.model.DistributionEntitlement;
import java.util.List;

public interface DistributionEntitlementRepository {

    List<DistributionEntitlement> saveAll(List<DistributionEntitlement> entitlements);

    List<DistributionEntitlement> findByDistributionId(String distributionId);
}
