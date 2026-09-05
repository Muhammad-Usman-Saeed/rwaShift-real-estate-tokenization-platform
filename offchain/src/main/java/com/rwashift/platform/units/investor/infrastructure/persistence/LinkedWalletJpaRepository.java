package com.rwashift.platform.units.investor.infrastructure.persistence;

import com.rwashift.platform.units.investor.domain.model.LinkedWallet;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface LinkedWalletJpaRepository extends JpaRepository<LinkedWallet, String> {

    List<LinkedWallet> findByInvestorId(String investorId);

    boolean existsByWalletAddress(String walletAddress);

    Optional<LinkedWallet> findByWalletAddress(String walletAddress);
}
