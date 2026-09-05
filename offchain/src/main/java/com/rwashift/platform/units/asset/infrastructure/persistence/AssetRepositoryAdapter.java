package com.rwashift.platform.units.asset.infrastructure.persistence;

import com.rwashift.platform.units.asset.domain.model.Asset;
import com.rwashift.platform.units.asset.domain.repository.AssetRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class AssetRepositoryAdapter implements AssetRepository {

    private final AssetJpaRepository jpaRepository;

    AssetRepositoryAdapter(AssetJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Asset save(Asset asset) {
        return jpaRepository.save(asset);
    }

    @Override
    public Optional<Asset> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Asset> findByOrganizationId(String organizationId) {
        return jpaRepository.findByOrganizationId(organizationId);
    }

    @Override
    public List<Asset> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public boolean existsByIdAndOrganizationId(String id, String organizationId) {
        return jpaRepository.existsByIdAndOrganizationId(id, organizationId);
    }

    @Override
    public boolean existsByOrganizationId(String organizationId) {
        return jpaRepository.existsByOrganizationId(organizationId);
    }
}
