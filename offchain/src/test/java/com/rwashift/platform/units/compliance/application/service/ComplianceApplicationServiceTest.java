package com.rwashift.platform.units.compliance.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rwashift.platform.units.audit.application.port.AuditPort;
import com.rwashift.platform.units.compliance.domain.model.EligibilityDecision;
import com.rwashift.platform.units.compliance.domain.model.EligibilityStatus;
import com.rwashift.platform.units.compliance.domain.policy.EligibilityRule;
import com.rwashift.platform.units.compliance.domain.policy.EligibilityRuleResult;
import com.rwashift.platform.units.compliance.domain.repository.EligibilityDecisionRepository;
import com.rwashift.platform.units.tokenization.application.port.IdentityRegistrationPort;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ComplianceApplicationServiceTest {

    private static final String INVESTOR_ID = "investor-1";
    private static final String OFFERING_ID = "offering-1";

    @Mock
    private EligibilityDecisionRepository decisionRepository;
    @Mock
    private IdentityRegistrationPort identityRegistrationPort;
    @Mock
    private AuditPort auditPort;

    @BeforeEach
    void stubSave() {
        when(decisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void allRulesPassingYieldsEligibleAndRegistersIdentityOnce() {
        when(decisionRepository.findByInvestorIdAndOfferingId(INVESTOR_ID, OFFERING_ID)).thenReturn(Optional.empty());
        EligibilityRule passingRule = (investorId, offeringId) -> EligibilityRuleResult.pass("OK");
        ComplianceApplicationService service =
                new ComplianceApplicationService(decisionRepository, List.of(passingRule), identityRegistrationPort, auditPort);

        EligibilityDecision decision = service.evaluateEligibility(INVESTOR_ID, OFFERING_ID);

        assertThat(decision.getStatus()).isEqualTo(EligibilityStatus.ELIGIBLE);
        verify(identityRegistrationPort, times(1)).registerInvestorIdentity(INVESTOR_ID, OFFERING_ID);
    }

    @Test
    void anyFailingRuleYieldsIneligibleAndNeverRegistersIdentity() {
        when(decisionRepository.findByInvestorIdAndOfferingId(INVESTOR_ID, OFFERING_ID)).thenReturn(Optional.empty());
        EligibilityRule passingRule = (investorId, offeringId) -> EligibilityRuleResult.pass("OK");
        EligibilityRule failingRule = (investorId, offeringId) -> EligibilityRuleResult.fail("KYC_NOT_VERIFIED");
        ComplianceApplicationService service = new ComplianceApplicationService(
                decisionRepository, List.of(passingRule, failingRule), identityRegistrationPort, auditPort);

        EligibilityDecision decision = service.evaluateEligibility(INVESTOR_ID, OFFERING_ID);

        assertThat(decision.getStatus()).isEqualTo(EligibilityStatus.INELIGIBLE);
        assertThat(decision.getReasonCodes()).contains("KYC_NOT_VERIFIED");
        verify(identityRegistrationPort, never()).registerInvestorIdentity(any(), any());
    }

    @Test
    void reEvaluatingAlreadyEligibleDecisionDoesNotReRegisterIdentity() {
        EligibilityDecision existing = new EligibilityDecision(INVESTOR_ID, OFFERING_ID);
        existing.recordEvaluation(EligibilityStatus.ELIGIBLE, List.of("OK"));
        existing.markIdentityRegistered();
        when(decisionRepository.findByInvestorIdAndOfferingId(INVESTOR_ID, OFFERING_ID)).thenReturn(Optional.of(existing));
        EligibilityRule passingRule = (investorId, offeringId) -> EligibilityRuleResult.pass("OK");
        ComplianceApplicationService service =
                new ComplianceApplicationService(decisionRepository, List.of(passingRule), identityRegistrationPort, auditPort);

        service.evaluateEligibility(INVESTOR_ID, OFFERING_ID);

        verify(identityRegistrationPort, never()).registerInvestorIdentity(any(), any());
    }
}
