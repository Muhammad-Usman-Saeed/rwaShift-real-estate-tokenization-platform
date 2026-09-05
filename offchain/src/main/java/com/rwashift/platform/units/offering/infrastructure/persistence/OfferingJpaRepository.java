package com.rwashift.platform.units.offering.infrastructure.persistence;

import com.rwashift.platform.units.offering.domain.model.Offering;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface OfferingJpaRepository extends JpaRepository<Offering, String> {

    List<Offering> findByOrganizationId(String organizationId);
}
