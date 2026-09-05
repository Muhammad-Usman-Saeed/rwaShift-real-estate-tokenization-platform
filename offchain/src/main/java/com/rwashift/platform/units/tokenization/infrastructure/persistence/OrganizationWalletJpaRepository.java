package com.rwashift.platform.units.tokenization.infrastructure.persistence;

import com.rwashift.platform.units.tokenization.domain.model.OrganizationWallet;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrganizationWalletJpaRepository extends JpaRepository<OrganizationWallet, String> {

    Optional<OrganizationWallet> findByOrganizationId(String organizationId);
}
