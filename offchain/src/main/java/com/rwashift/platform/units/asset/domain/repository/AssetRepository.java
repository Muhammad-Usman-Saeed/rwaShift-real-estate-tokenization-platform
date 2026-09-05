package com.rwashift.platform.units.asset.domain.repository;

import com.rwashift.platform.units.asset.domain.model.Asset;
import java.util.List;
import java.util.Optional;

public interface AssetRepository {

    Asset save(Asset asset);

    Optional<Asset> findById(String id);

    List<Asset> findByOrganizationId(String organizationId);

    List<Asset> findAll();

    boolean existsByIdAndOrganizationId(String id, String organizationId);

    boolean existsByOrganizationId(String organizationId);
}
