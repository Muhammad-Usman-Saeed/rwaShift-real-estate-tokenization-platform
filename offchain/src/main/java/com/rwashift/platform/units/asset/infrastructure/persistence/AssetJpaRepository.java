package com.rwashift.platform.units.asset.infrastructure.persistence;

import com.rwashift.platform.units.asset.domain.model.Asset;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface AssetJpaRepository extends JpaRepository<Asset, String> {

    List<Asset> findByOrganizationId(String organizationId);

    boolean existsByIdAndOrganizationId(String id, String organizationId);

    boolean existsByOrganizationId(String organizationId);
}
