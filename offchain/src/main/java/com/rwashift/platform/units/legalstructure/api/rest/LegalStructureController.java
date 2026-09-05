package com.rwashift.platform.units.legalstructure.api.rest;

import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.legalstructure.api.dto.CreateLegalStructureRequest;
import com.rwashift.platform.units.legalstructure.api.dto.LegalStructureResponse;
import com.rwashift.platform.units.legalstructure.api.dto.UpdateLegalStructureRequest;
import com.rwashift.platform.units.legalstructure.application.command.CreateLegalStructureCommand;
import com.rwashift.platform.units.legalstructure.application.command.UpdateLegalStructureCommand;
import com.rwashift.platform.units.legalstructure.application.service.LegalStructureApplicationService;
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
@RequestMapping("/api/v1/legal-structures")
public class LegalStructureController {

    private final LegalStructureApplicationService legalStructureApplicationService;

    public LegalStructureController(LegalStructureApplicationService legalStructureApplicationService) {
        this.legalStructureApplicationService = legalStructureApplicationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ISSUER_ADMIN','ORGANIZATION_ADMIN')")
    public ResponseEntity<LegalStructureResponse> create(TenantContext tenantContext,
            @Valid @RequestBody CreateLegalStructureRequest request) {
        var legalStructure = legalStructureApplicationService.createLegalStructure(tenantContext,
                new CreateLegalStructureCommand(request.assetId(), request.legalEntityName(), request.entityType(),
                        request.jurisdiction(), request.registrationNumber(), request.relationshipToAsset(),
                        request.investmentInstrumentType()));
        return ResponseEntity.created(URI.create("/api/v1/legal-structures/" + legalStructure.getId()))
                .body(LegalStructureResponse.from(legalStructure));
    }

    @PutMapping("/{legalStructureId}")
    @PreAuthorize("hasAnyRole('ISSUER_ADMIN','ORGANIZATION_ADMIN')")
    public LegalStructureResponse update(TenantContext tenantContext, @PathVariable String legalStructureId,
            @Valid @RequestBody UpdateLegalStructureRequest request) {
        var legalStructure = legalStructureApplicationService.updateLegalStructure(tenantContext, legalStructureId,
                new UpdateLegalStructureCommand(request.legalEntityName(), request.entityType(), request.jurisdiction(),
                        request.registrationNumber(), request.relationshipToAsset(), request.investmentInstrumentType()));
        return LegalStructureResponse.from(legalStructure);
    }

    @PostMapping("/{legalStructureId}/activate")
    @PreAuthorize("hasAnyRole('ISSUER_ADMIN','ORGANIZATION_ADMIN')")
    public LegalStructureResponse activate(TenantContext tenantContext, @PathVariable String legalStructureId) {
        return LegalStructureResponse.from(legalStructureApplicationService.activate(tenantContext, legalStructureId));
    }

    @GetMapping("/{legalStructureId}")
    public LegalStructureResponse get(TenantContext tenantContext, @PathVariable String legalStructureId) {
        return LegalStructureResponse.from(legalStructureApplicationService.getLegalStructure(tenantContext, legalStructureId));
    }

    @GetMapping
    public List<LegalStructureResponse> list(TenantContext tenantContext) {
        return legalStructureApplicationService.listLegalStructures(tenantContext).stream()
                .map(LegalStructureResponse::from)
                .toList();
    }
}
