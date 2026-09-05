package com.rwashift.platform.units.distribution.api.rest;

import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.distribution.api.dto.CreateDistributionRequest;
import com.rwashift.platform.units.distribution.api.dto.DistributionEntitlementResponse;
import com.rwashift.platform.units.distribution.api.dto.DistributionResponse;
import com.rwashift.platform.units.distribution.application.command.CreateDistributionCommand;
import com.rwashift.platform.units.distribution.application.service.DistributionApplicationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/distributions")
@PreAuthorize("hasAnyRole('ISSUER_ADMIN','ORGANIZATION_ADMIN','COMPLIANCE_OFFICER','PLATFORM_ADMIN')")
public class DistributionController {

    private final DistributionApplicationService distributionApplicationService;

    public DistributionController(DistributionApplicationService distributionApplicationService) {
        this.distributionApplicationService = distributionApplicationService;
    }

    @PostMapping
    public ResponseEntity<DistributionResponse> create(TenantContext tenantContext, @Valid @RequestBody CreateDistributionRequest r) {
        var distribution = distributionApplicationService.create(tenantContext,
                new CreateDistributionCommand(r.offeringId(), r.totalAmount(), r.currency(), r.recordDate()));
        return ResponseEntity.created(URI.create("/api/v1/distributions/" + distribution.getId())).body(DistributionResponse.from(distribution));
    }

    @PostMapping("/{distributionId}/calculate")
    public DistributionResponse calculate(TenantContext tenantContext, @PathVariable String distributionId) {
        return DistributionResponse.from(distributionApplicationService.calculate(tenantContext, distributionId));
    }

    @PostMapping("/{distributionId}/approve")
    public DistributionResponse approve(TenantContext tenantContext, @PathVariable String distributionId) {
        return DistributionResponse.from(distributionApplicationService.approve(tenantContext, distributionId));
    }

    @PostMapping("/{distributionId}/process")
    public DistributionResponse process(TenantContext tenantContext, @PathVariable String distributionId) {
        return DistributionResponse.from(distributionApplicationService.processSettlement(tenantContext, distributionId));
    }

    @PostMapping("/{distributionId}/complete")
    public DistributionResponse complete(TenantContext tenantContext, @PathVariable String distributionId) {
        return DistributionResponse.from(distributionApplicationService.complete(tenantContext, distributionId));
    }

    @GetMapping("/{distributionId}")
    @PreAuthorize("hasAnyRole('ISSUER_ADMIN','ORGANIZATION_ADMIN','COMPLIANCE_OFFICER','PLATFORM_ADMIN','INVESTOR')")
    public DistributionResponse get(TenantContext tenantContext, @PathVariable String distributionId) {
        return DistributionResponse.from(distributionApplicationService.getDistribution(tenantContext, distributionId));
    }

    @GetMapping("/{distributionId}/entitlements")
    @PreAuthorize("hasAnyRole('ISSUER_ADMIN','ORGANIZATION_ADMIN','COMPLIANCE_OFFICER','PLATFORM_ADMIN','INVESTOR')")
    public List<DistributionEntitlementResponse> entitlements(@PathVariable String distributionId) {
        return distributionApplicationService.getEntitlements(distributionId).stream()
                .map(DistributionEntitlementResponse::from)
                .toList();
    }

    @GetMapping
    public List<DistributionResponse> list(TenantContext tenantContext) {
        return distributionApplicationService.listForOrganization(tenantContext).stream()
                .map(DistributionResponse::from)
                .toList();
    }

    /** Investor-facing: distributions declared on a specific offering (e.g. one they hold units in). */
    @GetMapping("/offerings/{offeringId}")
    @PreAuthorize("hasAnyRole('ISSUER_ADMIN','ORGANIZATION_ADMIN','COMPLIANCE_OFFICER','PLATFORM_ADMIN','INVESTOR')")
    public List<DistributionResponse> forOffering(@PathVariable String offeringId) {
        return distributionApplicationService.listForOffering(offeringId).stream()
                .map(DistributionResponse::from)
                .toList();
    }
}
