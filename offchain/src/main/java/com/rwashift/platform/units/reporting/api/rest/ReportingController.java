package com.rwashift.platform.units.reporting.api.rest;

import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.reporting.api.dto.InvestorSummaryResponse;
import com.rwashift.platform.units.reporting.api.dto.IssuerSummaryResponse;
import com.rwashift.platform.units.reporting.application.service.ReportingApplicationService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportingController {

    private final ReportingApplicationService reportingApplicationService;

    public ReportingController(ReportingApplicationService reportingApplicationService) {
        this.reportingApplicationService = reportingApplicationService;
    }

    @GetMapping("/issuer/summary")
    @PreAuthorize("hasAnyRole('ISSUER_ADMIN','ISSUER_OPERATOR','ORGANIZATION_ADMIN','PLATFORM_ADMIN')")
    public IssuerSummaryResponse issuerSummary(TenantContext tenantContext) {
        return reportingApplicationService.issuerSummary(tenantContext);
    }

    @GetMapping("/investors/{investorId}/summary")
    public InvestorSummaryResponse investorSummary(TenantContext tenantContext, @PathVariable String investorId) {
        if (!investorId.equals(tenantContext.investorId()) && !tenantContext.isPlatformAdmin()) {
            throw new AccessDeniedException("May only view your own investor summary");
        }
        return reportingApplicationService.investorSummary(investorId);
    }
}
