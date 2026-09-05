package com.rwashift.platform.units.distribution.domain.repository;

import com.rwashift.platform.units.distribution.domain.model.Distribution;
import java.util.List;
import java.util.Optional;

public interface DistributionRepository {

    Distribution save(Distribution distribution);

    Optional<Distribution> findById(String id);

    List<Distribution> findByOrganizationId(String organizationId);

    List<Distribution> findByOfferingId(String offeringId);
}
