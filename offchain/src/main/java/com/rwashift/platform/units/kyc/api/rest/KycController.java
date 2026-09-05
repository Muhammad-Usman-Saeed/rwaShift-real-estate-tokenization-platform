package com.rwashift.platform.units.kyc.api.rest;

import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.kyc.api.dto.KycCaseResponse;
import com.rwashift.platform.units.kyc.api.dto.RejectKycRequest;
import com.rwashift.platform.units.kyc.application.service.KycApplicationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/kyc")
public class KycController {

    private final KycApplicationService kycApplicationService;

    public KycController(KycApplicationService kycApplicationService) {
        this.kycApplicationService = kycApplicationService;
    }

    @PostMapping("/investors/{investorId}/submit")
    public KycCaseResponse submit(TenantContext tenantContext, @PathVariable String investorId) {
        if (!investorId.equals(tenantContext.investorId()) && !tenantContext.isPlatformAdmin()) {
            throw new AccessDeniedException("May only submit KYC for your own investor profile");
        }
        return KycCaseResponse.from(kycApplicationService.submit(investorId));
    }

    @PostMapping("/cases/{kycCaseId}/start-review")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER','PLATFORM_ADMIN')")
    public KycCaseResponse startReview(@PathVariable String kycCaseId) {
        return KycCaseResponse.from(kycApplicationService.startReview(kycCaseId));
    }

    @PostMapping("/cases/{kycCaseId}/verify")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER','PLATFORM_ADMIN')")
    public KycCaseResponse verify(TenantContext tenantContext, @PathVariable String kycCaseId) {
        return KycCaseResponse.from(kycApplicationService.verify(kycCaseId, tenantContext.userId()));
    }

    @PostMapping("/cases/{kycCaseId}/reject")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER','PLATFORM_ADMIN')")
    public KycCaseResponse reject(TenantContext tenantContext, @PathVariable String kycCaseId,
            @Valid @RequestBody RejectKycRequest request) {
        return KycCaseResponse.from(kycApplicationService.reject(kycCaseId, tenantContext.userId(), request.reason()));
    }

    @GetMapping("/investors/{investorId}")
    public KycCaseResponse getByInvestor(TenantContext tenantContext, @PathVariable String investorId) {
        if (!investorId.equals(tenantContext.investorId()) && !tenantContext.isPlatformAdmin()
                && !tenantContext.hasRole("COMPLIANCE_OFFICER")) {
            throw new AccessDeniedException("May only view your own KYC case");
        }
        return KycCaseResponse.from(kycApplicationService.getByInvestor(investorId));
    }

    /** Review queue: every KYC case, for compliance/admin. */
    @GetMapping("/cases")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER','PLATFORM_ADMIN')")
    public List<KycCaseResponse> listCases() {
        return kycApplicationService.listCases().stream().map(KycCaseResponse::from).toList();
    }
}
