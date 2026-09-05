package com.rwashift.platform.units.compliance.infrastructure.persistence;

import com.rwashift.platform.units.compliance.domain.model.EligibilityDecision;
import com.rwashift.platform.units.compliance.domain.repository.EligibilityDecisionRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class EligibilityDecisionRepositoryAdapter implements EligibilityDecisionRepository {

    private final EligibilityDecisionJpaRepository jpaRepository;

    EligibilityDecisionRepositoryAdapter(EligibilityDecisionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public EligibilityDecision save(EligibilityDecision decision) {
        return jpaRepository.save(decision);
    }

    @Override
    public Optional<EligibilityDecision> findByInvestorIdAndOfferingId(String investorId, String offeringId) {
        return jpaRepository.findByInvestorIdAndOfferingId(investorId, offeringId);
    }
}
