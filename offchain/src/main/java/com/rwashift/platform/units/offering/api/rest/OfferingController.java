package com.rwashift.platform.units.offering.api.rest;

import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.offering.api.dto.CreateOfferingRequest;
import com.rwashift.platform.units.offering.api.dto.OfferingResponse;
import com.rwashift.platform.units.offering.api.dto.UpdateOfferingRequest;
import com.rwashift.platform.units.offering.application.command.CreateOfferingCommand;
import com.rwashift.platform.units.offering.application.command.UpdateOfferingCommand;
import com.rwashift.platform.units.offering.application.service.OfferingApplicationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/offerings")
public class OfferingController {

    private final OfferingApplicationService offeringApplicationService;

    public OfferingController(OfferingApplicationService offeringApplicationService) {
        this.offeringApplicationService = offeringApplicationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ISSUER_ADMIN','ORGANIZATION_ADMIN')")
    public ResponseEntity<OfferingResponse> create(TenantContext tenantContext, @Valid @RequestBody CreateOfferingRequest r) {
        var offering = offeringApplicationService.createOffering(tenantContext, new CreateOfferingCommand(
                r.assetId(), r.legalStructureId(), r.name(), r.targetRaise(), r.currency(), r.totalUnits(),
                r.unitPrice(), r.minimumInvestment(), r.offeredInterestPercentage(), r.openingDate(), r.closingDate()));
        return ResponseEntity.created(URI.create("/api/v1/offerings/" + offering.getId())).body(OfferingResponse.from(offering));
    }

    @PutMapping("/{offeringId}")
    @PreAuthorize("hasAnyRole('ISSUER_ADMIN','ORGANIZATION_ADMIN')")
    public OfferingResponse update(TenantContext tenantContext, @PathVariable String offeringId,
            @Valid @RequestBody UpdateOfferingRequest r) {
        var offering = offeringApplicationService.updateOffering(tenantContext, offeringId, new UpdateOfferingCommand(
                r.name(), r.targetRaise(), r.currency(), r.totalUnits(), r.unitPrice(), r.minimumInvestment(),
                r.offeredInterestPercentage(), r.openingDate(), r.closingDate()));
        return OfferingResponse.from(offering);
    }

    @PostMapping("/{offeringId}/submit-for-review")
    @PreAuthorize("hasAnyRole('ISSUER_ADMIN','ORGANIZATION_ADMIN')")
    public OfferingResponse submitForReview(TenantContext tenantContext, @PathVariable String offeringId) {
        return OfferingResponse.from(offeringApplicationService.submitForReview(tenantContext, offeringId));
    }

    @PostMapping("/{offeringId}/approve")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER','PLATFORM_ADMIN')")
    public OfferingResponse approve(TenantContext tenantContext, @PathVariable String offeringId) {
        return OfferingResponse.from(offeringApplicationService.approve(tenantContext, offeringId));
    }

    @PostMapping("/{offeringId}/reject")
    @PreAuthorize("hasAnyRole('COMPLIANCE_OFFICER','PLATFORM_ADMIN')")
    public OfferingResponse reject(TenantContext tenantContext, @PathVariable String offeringId) {
        return OfferingResponse.from(offeringApplicationService.reject(tenantContext, offeringId));
    }

    @PostMapping("/{offeringId}/begin-tokenization")
    @PreAuthorize("hasAnyRole('ISSUER_ADMIN','ORGANIZATION_ADMIN')")
    public OfferingResponse beginTokenization(TenantContext tenantContext, @PathVariable String offeringId) {
        return OfferingResponse.from(offeringApplicationService.beginTokenization(tenantContext, offeringId));
    }

    @PostMapping("/{offeringId}/close")
    @PreAuthorize("hasAnyRole('ISSUER_ADMIN','ORGANIZATION_ADMIN')")
    public OfferingResponse close(TenantContext tenantContext, @PathVariable String offeringId) {
        return OfferingResponse.from(offeringApplicationService.close(tenantContext, offeringId));
    }

    @GetMapping("/{offeringId}")
    public OfferingResponse get(TenantContext tenantContext, @PathVariable String offeringId) {
        return OfferingResponse.from(offeringApplicationService.getOffering(tenantContext, offeringId));
    }

    @GetMapping
    public List<OfferingResponse> list(TenantContext tenantContext) {
        return offeringApplicationService.listOfferings(tenantContext).stream().map(OfferingResponse::from).toList();
    }
}
