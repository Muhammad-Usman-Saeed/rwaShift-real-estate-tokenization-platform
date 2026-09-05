package com.rwashift.platform.units.compliance.infrastructure.persistence;

import com.rwashift.platform.units.compliance.domain.model.EligibilityDecision;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface EligibilityDecisionJpaRepository extends JpaRepository<EligibilityDecision, String> {

    Optional<EligibilityDecision> findByInvestorIdAndOfferingId(String investorId, String offeringId);
}
