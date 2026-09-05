package com.rwashift.platform.units.compliance.api.dto;

import com.rwashift.platform.units.compliance.domain.model.EligibilityDecision;
import java.time.Instant;
import java.util.List;

public record EligibilityDecisionResponse(
        String investorId,
        String offeringId,
        String status,
        List<String> reasonCodes,
        Instant evaluatedAt,
        boolean identityRegistered
) {
    public static EligibilityDecisionResponse from(EligibilityDecision decision) {
        return new EligibilityDecisionResponse(
                decision.getInvestorId(), decision.getOfferingId(), decision.getStatus().name(),
                decision.getReasonCodes(), decision.getEvaluatedAt(), decision.isIdentityRegistered());
    }
}
