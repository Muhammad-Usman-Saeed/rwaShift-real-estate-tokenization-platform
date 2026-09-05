package com.rwashift.platform.units.tokenization.domain.repository;

import com.rwashift.platform.units.tokenization.domain.model.OrganizationWallet;
import java.util.List;
import java.util.Optional;

public interface OrganizationWalletRepository {

    OrganizationWallet save(OrganizationWallet wallet);

    Optional<OrganizationWallet> findByOrganizationId(String organizationId);

    /** Every organization wallet ever provisioned — admin "who needs funding" view. */
    List<OrganizationWallet> findAll();
}
