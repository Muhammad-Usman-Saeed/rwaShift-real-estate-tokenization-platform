package com.rwashift.platform.units.compliance.infrastructure.integration;

import com.rwashift.platform.units.compliance.domain.policy.EligibilityRule;
import com.rwashift.platform.units.compliance.domain.policy.EligibilityRuleResult;
import com.rwashift.platform.units.kyc.application.port.KycLookupPort;
import org.springframework.stereotype.Component;

@Component
class KycVerifiedEligibilityRule implements EligibilityRule {

    private final KycLookupPort kycLookupPort;

    KycVerifiedEligibilityRule(KycLookupPort kycLookupPort) {
        this.kycLookupPort = kycLookupPort;
    }

    @Override
    public EligibilityRuleResult evaluate(String investorId, String offeringId) {
        return kycLookupPort.isVerified(investorId)
                ? EligibilityRuleResult.pass("KYC_VERIFIED")
                : EligibilityRuleResult.fail("KYC_NOT_VERIFIED");
    }
}
