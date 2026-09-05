package com.rwashift.platform.units.investor.domain.repository;

import com.rwashift.platform.units.investor.domain.model.LinkedWallet;
import java.util.List;
import java.util.Optional;

public interface LinkedWalletRepository {

    LinkedWallet save(LinkedWallet wallet);

    List<LinkedWallet> findByInvestorId(String investorId);

    boolean existsByWalletAddress(String walletAddress);

    /** {@code wallet_address} is globally unique (DB constraint) — at most one row can ever match. */
    Optional<LinkedWallet> findByWalletAddress(String walletAddress);
}
