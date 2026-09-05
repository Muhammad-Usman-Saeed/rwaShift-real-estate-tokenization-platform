package com.rwashift.platform.units.offering.domain.repository;

import com.rwashift.platform.units.offering.domain.model.Offering;
import java.util.List;
import java.util.Optional;

public interface OfferingRepository {

    Offering save(Offering offering);

    Optional<Offering> findById(String id);

    List<Offering> findByOrganizationId(String organizationId);

    List<Offering> findAll();
}
