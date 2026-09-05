package com.rwashift.platform.units.ownership.api.rest;

import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.ownership.api.dto.OwnershipRecordResponse;
import com.rwashift.platform.units.ownership.application.service.OwnershipApplicationService;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ownership")
public class OwnershipController {

    private final OwnershipApplicationService ownershipApplicationService;

    public OwnershipController(OwnershipApplicationService ownershipApplicationService) {
        this.ownershipApplicationService = ownershipApplicationService;
    }

    @GetMapping("/offerings/{offeringId}")
    @PreAuthorize("hasAnyRole('ISSUER_ADMIN','ISSUER_OPERATOR','ORGANIZATION_ADMIN','COMPLIANCE_OFFICER','PLATFORM_ADMIN')")
    public List<OwnershipRecordResponse> forOffering(@PathVariable String offeringId) {
        return ownershipApplicationService.getOwnershipForOffering(offeringId).stream()
                .map(OwnershipRecordResponse::from)
                .toList();
    }

    @GetMapping("/investors/{investorId}")
    public List<OwnershipRecordResponse> forInvestor(TenantContext tenantContext, @PathVariable String investorId) {
        if (!investorId.equals(tenantContext.investorId()) && !tenantContext.isPlatformAdmin()) {
            throw new AccessDeniedException("May only view your own ownership records");
        }
        return ownershipApplicationService.getOwnershipForInvestor(investorId).stream()
                .map(OwnershipRecordResponse::from)
                .toList();
    }
}
