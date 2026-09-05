package com.rwashift.platform.units.audit.api.rest;

import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.audit.api.dto.AuditLogEntryResponse;
import com.rwashift.platform.units.audit.application.service.AuditApplicationService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/audit")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN','ORGANIZATION_ADMIN','COMPLIANCE_OFFICER')")
public class AuditController {

    private final AuditApplicationService auditApplicationService;

    public AuditController(AuditApplicationService auditApplicationService) {
        this.auditApplicationService = auditApplicationService;
    }

    /** Platform admins see the platform-wide trail (including system-actor entries with no organization); everyone else sees their own organization's. */
    @GetMapping
    public List<AuditLogEntryResponse> forOrganization(TenantContext tenantContext) {
        List<com.rwashift.platform.units.audit.domain.model.AuditLogEntry> entries = tenantContext.isPlatformAdmin()
                ? auditApplicationService.getAll()
                : auditApplicationService.getForOrganization(tenantContext.organizationId());
        return entries.stream().map(AuditLogEntryResponse::from).toList();
    }

    @GetMapping("/resources/{resourceType}/{resourceId}")
    public List<AuditLogEntryResponse> forResource(@PathVariable String resourceType, @PathVariable String resourceId) {
        return auditApplicationService.getForResource(resourceType, resourceId).stream()
                .map(AuditLogEntryResponse::from)
                .toList();
    }
}
