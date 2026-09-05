package com.rwashift.platform.units.organization.domain.repository;

import com.rwashift.platform.units.organization.domain.model.Organization;
import java.util.List;
import java.util.Optional;

public interface OrganizationRepository {

    Organization save(Organization organization);

    Optional<Organization> findById(String id);

    List<Organization> findAll();

    boolean existsById(String id);

    void deleteById(String id);
}
