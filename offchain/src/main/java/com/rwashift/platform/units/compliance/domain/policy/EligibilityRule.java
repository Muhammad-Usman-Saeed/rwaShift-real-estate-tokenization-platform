package com.rwashift.platform.units.compliance.domain.policy;

/**
 * A single, independently pluggable eligibility check. All Spring beans implementing this
 * interface are auto-collected and evaluated by {@code ComplianceApplicationService} — adding a
 * jurisdiction, classification, or max-ownership rule later means adding a new bean, not
 * modifying this unit's core logic or hardcoding regulation into the application.
 */
public interface EligibilityRule {

    EligibilityRuleResult evaluate(String investorId, String offeringId);
}
