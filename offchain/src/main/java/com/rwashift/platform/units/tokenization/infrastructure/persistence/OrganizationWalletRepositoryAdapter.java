package com.rwashift.platform.units.tokenization.infrastructure.persistence;

import com.rwashift.platform.units.tokenization.domain.model.OrganizationWallet;
import com.rwashift.platform.units.tokenization.domain.repository.OrganizationWalletRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class OrganizationWalletRepositoryAdapter implements OrganizationWalletRepository {

    private final OrganizationWalletJpaRepository jpaRepository;

    OrganizationWalletRepositoryAdapter(OrganizationWalletJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public OrganizationWallet save(OrganizationWallet wallet) {
        return jpaRepository.save(wallet);
    }

    @Override
    public Optional<OrganizationWallet> findByOrganizationId(String organizationId) {
        return jpaRepository.findByOrganizationId(organizationId);
    }

    @Override
    public List<OrganizationWallet> findAll() {
        return jpaRepository.findAll();
    }
}
