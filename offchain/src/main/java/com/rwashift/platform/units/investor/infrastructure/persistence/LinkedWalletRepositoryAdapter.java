package com.rwashift.platform.units.investor.infrastructure.persistence;

import com.rwashift.platform.units.investor.domain.model.LinkedWallet;
import com.rwashift.platform.units.investor.domain.repository.LinkedWalletRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class LinkedWalletRepositoryAdapter implements LinkedWalletRepository {

    private final LinkedWalletJpaRepository jpaRepository;

    LinkedWalletRepositoryAdapter(LinkedWalletJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public LinkedWallet save(LinkedWallet wallet) {
        return jpaRepository.save(wallet);
    }

    @Override
    public List<LinkedWallet> findByInvestorId(String investorId) {
        return jpaRepository.findByInvestorId(investorId);
    }

    @Override
    public boolean existsByWalletAddress(String walletAddress) {
        return jpaRepository.existsByWalletAddress(walletAddress);
    }

    @Override
    public Optional<LinkedWallet> findByWalletAddress(String walletAddress) {
        return jpaRepository.findByWalletAddress(walletAddress);
    }
}
