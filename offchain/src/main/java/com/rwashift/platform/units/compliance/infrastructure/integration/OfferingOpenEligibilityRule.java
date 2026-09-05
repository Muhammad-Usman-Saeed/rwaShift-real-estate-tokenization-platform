package com.rwashift.platform.units.compliance.infrastructure.integration;

import com.rwashift.platform.units.compliance.domain.policy.EligibilityRule;
import com.rwashift.platform.units.compliance.domain.policy.EligibilityRuleResult;
import com.rwashift.platform.units.offering.application.port.OfferingLookupPort;
import org.springframework.stereotype.Component;

@Component
class OfferingOpenEligibilityRule implements EligibilityRule {

    private final OfferingLookupPort offeringLookupPort;

    OfferingOpenEligibilityRule(OfferingLookupPort offeringLookupPort) {
        this.offeringLookupPort = offeringLookupPort;
    }

    @Override
    public EligibilityRuleResult evaluate(String investorId, String offeringId) {
        return offeringLookupPort.findSnapshot(offeringId)
                .filter(snapshot -> snapshot.openForInvestment() && snapshot.unitsAvailable() > 0)
                .map(snapshot -> EligibilityRuleResult.pass("OFFERING_OPEN"))
                .orElseGet(() -> EligibilityRuleResult.fail("OFFERING_NOT_OPEN_OR_FULL"));
    }
}
