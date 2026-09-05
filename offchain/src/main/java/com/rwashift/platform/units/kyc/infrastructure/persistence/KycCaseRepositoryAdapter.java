package com.rwashift.platform.units.kyc.infrastructure.persistence;

import com.rwashift.platform.units.kyc.domain.model.KycCase;
import com.rwashift.platform.units.kyc.domain.repository.KycCaseRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class KycCaseRepositoryAdapter implements KycCaseRepository {

    private final KycCaseJpaRepository jpaRepository;

    KycCaseRepositoryAdapter(KycCaseJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public KycCase save(KycCase kycCase) {
        return jpaRepository.save(kycCase);
    }

    @Override
    public Optional<KycCase> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<KycCase> findByInvestorId(String investorId) {
        return jpaRepository.findByInvestorId(investorId);
    }

    @Override
    public List<KycCase> findAll() {
        return jpaRepository.findAll();
    }
}
