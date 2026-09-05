package com.rwashift.platform.units.investor.infrastructure.persistence;

import com.rwashift.platform.units.investor.domain.model.Investor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface InvestorJpaRepository extends JpaRepository<Investor, String> {

    Optional<Investor> findByUserId(String userId);

    Optional<Investor> findByPrimaryWalletAddress(String walletAddress);
}
