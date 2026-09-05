package com.rwashift.platform.units.organization.api.rest;

import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.organization.api.dto.CreateOrganizationRequest;
import com.rwashift.platform.units.organization.api.dto.OrganizationResponse;
import com.rwashift.platform.units.organization.api.dto.UpdateOrganizationRequest;
import com.rwashift.platform.units.organization.application.command.CreateOrganizationCommand;
import com.rwashift.platform.units.organization.application.command.UpdateOrganizationCommand;
import com.rwashift.platform.units.organization.application.service.OrganizationApplicationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationApplicationService organizationApplicationService;

    public OrganizationController(OrganizationApplicationService organizationApplicationService) {
        this.organizationApplicationService = organizationApplicationService;
    }

    @PostMapping
    public ResponseEntity<OrganizationResponse> create(TenantContext tenantContext,
            @Valid @RequestBody CreateOrganizationRequest request) {
        var organization = organizationApplicationService.createOrganization(tenantContext,
                new CreateOrganizationCommand(request.legalName(), request.displayName(), request.countryCode()));
        OrganizationResponse body = OrganizationResponse.from(organization);
        return ResponseEntity.created(URI.create("/api/v1/organizations/" + organization.getId())).body(body);
    }

    @GetMapping("/{organizationId}")
    public OrganizationResponse get(TenantContext tenantContext, @PathVariable String organizationId) {
        return OrganizationResponse.from(organizationApplicationService.getOrganization(tenantContext, organizationId));
    }

    @PutMapping("/{organizationId}")
    public OrganizationResponse update(TenantContext tenantContext, @PathVariable String organizationId,
            @Valid @RequestBody UpdateOrganizationRequest request) {
        var organization = organizationApplicationService.updateOrganization(tenantContext, organizationId,
                new UpdateOrganizationCommand(request.legalName(), request.displayName()));
        return OrganizationResponse.from(organization);
    }

    @DeleteMapping("/{organizationId}")
    public ResponseEntity<Void> delete(TenantContext tenantContext, @PathVariable String organizationId) {
        organizationApplicationService.deleteOrganization(tenantContext, organizationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<OrganizationResponse> list(TenantContext tenantContext) {
        return organizationApplicationService.listVisibleOrganizations(tenantContext).stream()
                .map(OrganizationResponse::from)
                .toList();
    }
}
