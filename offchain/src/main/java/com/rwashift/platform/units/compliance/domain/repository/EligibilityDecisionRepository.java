package com.rwashift.platform.units.compliance.domain.repository;

import com.rwashift.platform.units.compliance.domain.model.EligibilityDecision;
import java.util.Optional;

public interface EligibilityDecisionRepository {

    EligibilityDecision save(EligibilityDecision decision);

    Optional<EligibilityDecision> findByInvestorIdAndOfferingId(String investorId, String offeringId);
}
