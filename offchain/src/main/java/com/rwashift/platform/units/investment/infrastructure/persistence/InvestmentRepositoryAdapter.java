package com.rwashift.platform.units.investment.infrastructure.persistence;

import com.rwashift.platform.units.investment.domain.model.Investment;
import com.rwashift.platform.units.investment.domain.repository.InvestmentRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class InvestmentRepositoryAdapter implements InvestmentRepository {

    private final InvestmentJpaRepository jpaRepository;

    InvestmentRepositoryAdapter(InvestmentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Investment save(Investment investment) {
        return jpaRepository.save(investment);
    }

    @Override
    public Optional<Investment> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Investment> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey);
    }

    @Override
    public List<Investment> findByInvestorId(String investorId) {
        return jpaRepository.findByInvestorId(investorId);
    }

    @Override
    public List<Investment> findByOrganizationId(String organizationId) {
        return jpaRepository.findByOrganizationId(organizationId);
    }

    @Override
    public List<Investment> findByOfferingId(String offeringId) {
        return jpaRepository.findByOfferingId(offeringId);
    }

    @Override
    public List<Investment> findAll() {
        return jpaRepository.findAll();
    }
}
