package com.rwashift.platform.units.compliance.api.rest;

import com.rwashift.platform.units.compliance.api.dto.EligibilityDecisionResponse;
import com.rwashift.platform.units.compliance.application.service.ComplianceApplicationService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/compliance/eligibility")
public class ComplianceController {

    private final ComplianceApplicationService complianceApplicationService;

    public ComplianceController(ComplianceApplicationService complianceApplicationService) {
        this.complianceApplicationService = complianceApplicationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER','PLATFORM_ADMIN','ISSUER_ADMIN')")
    public EligibilityDecisionResponse evaluate(@RequestParam String investorId, @RequestParam String offeringId) {
        return EligibilityDecisionResponse.from(complianceApplicationService.evaluateEligibility(investorId, offeringId));
    }

    @GetMapping
    public EligibilityDecisionResponse get(@RequestParam String investorId, @RequestParam String offeringId) {
        return EligibilityDecisionResponse.from(complianceApplicationService.getDecision(investorId, offeringId));
    }
}
