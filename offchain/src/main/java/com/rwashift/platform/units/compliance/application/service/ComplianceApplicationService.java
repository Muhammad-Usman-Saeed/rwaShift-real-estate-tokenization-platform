package com.rwashift.platform.units.compliance.application.service;

import com.rwashift.platform.shared.domain.NotFoundException;
import com.rwashift.platform.units.audit.application.port.AuditPort;
import com.rwashift.platform.units.compliance.application.port.EligibilityLookupPort;
import com.rwashift.platform.units.compliance.application.port.IdentityRegistrationConfirmationPort;
import com.rwashift.platform.units.compliance.domain.model.EligibilityDecision;
import com.rwashift.platform.units.compliance.domain.model.EligibilityStatus;
import com.rwashift.platform.units.compliance.domain.policy.EligibilityRule;
import com.rwashift.platform.units.compliance.domain.policy.EligibilityRuleResult;
import com.rwashift.platform.units.compliance.domain.repository.EligibilityDecisionRepository;
import com.rwashift.platform.units.tokenization.application.port.IdentityRegistrationPort;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Runs every registered {@link EligibilityRule} for an (investor, offering) pair. All rules
 * must pass for {@code ELIGIBLE}; any hard failure yields {@code INELIGIBLE}. When the decision
 * newly becomes {@code ELIGIBLE}, this service — and only this service — asks Tokenization to
 * register the investor's on-chain identity (see {@link IdentityRegistrationPort}); it never
 * calls Web3j itself.
 */
@Service
public class ComplianceApplicationService implements EligibilityLookupPort, IdentityRegistrationConfirmationPort {

    private final EligibilityDecisionRepository decisionRepository;
    private final List<EligibilityRule> rules;
    private final IdentityRegistrationPort identityRegistrationPort;
    private final AuditPort auditPort;

    public ComplianceApplicationService(EligibilityDecisionRepository decisionRepository, List<EligibilityRule> rules,
            IdentityRegistrationPort identityRegistrationPort, AuditPort auditPort) {
        this.decisionRepository = decisionRepository;
        this.rules = rules;
        this.identityRegistrationPort = identityRegistrationPort;
        this.auditPort = auditPort;
    }

    @Transactional
    public EligibilityDecision evaluateEligibility(String investorId, String offeringId) {
        EligibilityDecision decision = decisionRepository.findByInvestorIdAndOfferingId(investorId, offeringId)
                .orElseGet(() -> new EligibilityDecision(investorId, offeringId));
        EligibilityStatus previousStatus = decision.getStatus();

        List<String> reasonCodes = new ArrayList<>();
        boolean anyFailed = false;
        for (EligibilityRule rule : rules) {
            EligibilityRuleResult result = rule.evaluate(investorId, offeringId);
            reasonCodes.add(result.reasonCode());
            if (!result.satisfied()) {
                anyFailed = true;
            }
        }
        EligibilityStatus newStatus = anyFailed ? EligibilityStatus.INELIGIBLE : EligibilityStatus.ELIGIBLE;
        decision.recordEvaluation(newStatus, reasonCodes);
        decision = decisionRepository.save(decision);
        auditPort.record(AuditPort.AuditEntry.of(null, null, "ELIGIBILITY_EVALUATED", "EligibilityDecision",
                investorId + ":" + offeringId, previousStatus.name(), newStatus.name()));

        // Deliberately keyed on "eligible AND not yet confirmed" rather than "just became
        // eligible" — a registration that reverted on-chain must be retried on the next
        // evaluation, not just the one where eligibility first flipped. See
        // #onIdentityRegistrationConfirmed for the only place identityRegistered is ever set true.
        if (newStatus == EligibilityStatus.ELIGIBLE && !decision.isIdentityRegistered()) {
            identityRegistrationPort.registerInvestorIdentity(investorId, offeringId);
        }
        return decision;
    }

    /** Called by Tokenization once the on-chain identity registration transaction actually confirms — never at submission time. */
    @Override
    @Transactional
    public void onIdentityRegistrationConfirmed(String investorId, String offeringId) {
        decisionRepository.findByInvestorIdAndOfferingId(investorId, offeringId).ifPresent(decision -> {
            decision.markIdentityRegistered();
            decisionRepository.save(decision);
        });
    }

    @Transactional(readOnly = true)
    public EligibilityDecision getDecision(String investorId, String offeringId) {
        return decisionRepository.findByInvestorIdAndOfferingId(investorId, offeringId)
                .orElseThrow(() -> new NotFoundException("EligibilityDecision", investorId + "/" + offeringId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEligible(String investorId, String offeringId) {
        return decisionRepository.findByInvestorIdAndOfferingId(investorId, offeringId)
                .map(d -> d.getStatus() == EligibilityStatus.ELIGIBLE)
                .orElse(false);
    }

    @Override
    @Transactional
    public boolean evaluate(String investorId, String offeringId) {
        return evaluateEligibility(investorId, offeringId).getStatus() == EligibilityStatus.ELIGIBLE;
    }
}
