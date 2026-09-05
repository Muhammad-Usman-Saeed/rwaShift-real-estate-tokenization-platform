package com.rwashift.platform.units.distribution.infrastructure.persistence;

import com.rwashift.platform.units.distribution.domain.model.Distribution;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface DistributionJpaRepository extends JpaRepository<Distribution, String> {

    List<Distribution> findByOrganizationId(String organizationId);

    List<Distribution> findByOfferingId(String offeringId);
}
