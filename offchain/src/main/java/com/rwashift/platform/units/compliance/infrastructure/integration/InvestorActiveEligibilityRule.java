package com.rwashift.platform.units.compliance.infrastructure.integration;

import com.rwashift.platform.units.compliance.domain.policy.EligibilityRule;
import com.rwashift.platform.units.compliance.domain.policy.EligibilityRuleResult;
import com.rwashift.platform.units.investor.application.port.InvestorLookupPort;
import org.springframework.stereotype.Component;

@Component
class InvestorActiveEligibilityRule implements EligibilityRule {

    private final InvestorLookupPort investorLookupPort;

    InvestorActiveEligibilityRule(InvestorLookupPort investorLookupPort) {
        this.investorLookupPort = investorLookupPort;
    }

    @Override
    public EligibilityRuleResult evaluate(String investorId, String offeringId) {
        return investorLookupPort.findSnapshot(investorId)
                .filter(snapshot -> snapshot.active() && snapshot.primaryWalletAddress() != null)
                .map(snapshot -> EligibilityRuleResult.pass("INVESTOR_ACTIVE_WITH_WALLET"))
                .orElseGet(() -> EligibilityRuleResult.fail("INVESTOR_NOT_ACTIVE_OR_NO_WALLET"));
    }
}
