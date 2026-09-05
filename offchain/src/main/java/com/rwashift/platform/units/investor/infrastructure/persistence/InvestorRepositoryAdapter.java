package com.rwashift.platform.units.investor.infrastructure.persistence;

import com.rwashift.platform.units.investor.domain.model.Investor;
import com.rwashift.platform.units.investor.domain.repository.InvestorRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class InvestorRepositoryAdapter implements InvestorRepository {

    private final InvestorJpaRepository jpaRepository;

    InvestorRepositoryAdapter(InvestorJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Investor save(Investor investor) {
        return jpaRepository.save(investor);
    }

    @Override
    public Optional<Investor> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Investor> findByUserId(String userId) {
        return jpaRepository.findByUserId(userId);
    }

    @Override
    public Optional<Investor> findByPrimaryWalletAddress(String walletAddress) {
        return jpaRepository.findByPrimaryWalletAddress(walletAddress);
    }

    @Override
    public List<Investor> findAll() {
        return jpaRepository.findAll();
    }
}
