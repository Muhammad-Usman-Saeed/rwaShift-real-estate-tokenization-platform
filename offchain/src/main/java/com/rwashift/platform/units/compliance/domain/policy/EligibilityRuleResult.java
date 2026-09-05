package com.rwashift.platform.units.compliance.domain.policy;

public record EligibilityRuleResult(boolean satisfied, String reasonCode) {

    public static EligibilityRuleResult pass(String reasonCode) {
        return new EligibilityRuleResult(true, reasonCode);
    }

    public static EligibilityRuleResult fail(String reasonCode) {
        return new EligibilityRuleResult(false, reasonCode);
    }
}
