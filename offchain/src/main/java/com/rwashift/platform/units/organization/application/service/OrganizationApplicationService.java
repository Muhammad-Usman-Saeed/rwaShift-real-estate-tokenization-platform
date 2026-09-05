package com.rwashift.platform.units.organization.application.service;

import com.rwashift.platform.shared.domain.DomainException;
import com.rwashift.platform.shared.domain.NotFoundException;
import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.asset.application.port.AssetLookupPort;
import com.rwashift.platform.units.audit.application.port.AuditPort;
import com.rwashift.platform.units.organization.application.command.CreateOrganizationCommand;
import com.rwashift.platform.units.organization.application.command.UpdateOrganizationCommand;
import com.rwashift.platform.units.organization.application.port.OrganizationLookupPort;
import com.rwashift.platform.units.organization.domain.model.Organization;
import com.rwashift.platform.units.organization.domain.repository.OrganizationRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationApplicationService implements OrganizationLookupPort {

    private final OrganizationRepository organizationRepository;
    private final AssetLookupPort assetLookupPort;
    private final AuditPort auditPort;

    public OrganizationApplicationService(OrganizationRepository organizationRepository, AssetLookupPort assetLookupPort,
            AuditPort auditPort) {
        this.organizationRepository = organizationRepository;
        this.assetLookupPort = assetLookupPort;
        this.auditPort = auditPort;
    }

    @Transactional
    public Organization createOrganization(TenantContext caller, CreateOrganizationCommand command) {
        if (!caller.isPlatformAdmin()) {
            throw new AccessDeniedException("Only PLATFORM_ADMIN may create organizations");
        }
        Organization organization = new Organization(command.legalName(), command.displayName(), command.countryCode());
        organization = organizationRepository.save(organization);
        auditPort.record(AuditPort.AuditEntry.of(caller.userId(), organization.getId(), "ORGANIZATION_CREATED",
                "Organization", organization.getId(), null, organization.getStatus().name()));
        return organization;
    }

    @Transactional
    public Organization updateOrganization(TenantContext caller, String organizationId, UpdateOrganizationCommand command) {
        if (!caller.isPlatformAdmin()) {
            throw new AccessDeniedException("Only PLATFORM_ADMIN may update organizations");
        }
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization", organizationId));
        organization.rename(command.legalName(), command.displayName());
        organization = organizationRepository.save(organization);
        auditPort.record(AuditPort.AuditEntry.of(caller.userId(), organizationId, "ORGANIZATION_UPDATED",
                "Organization", organizationId, null, organization.getDisplayName()));
        return organization;
    }

    /** Refuses to delete an organization that still has assets attached — see {@link AssetLookupPort}. */
    @Transactional
    public void deleteOrganization(TenantContext caller, String organizationId) {
        if (!caller.isPlatformAdmin()) {
            throw new AccessDeniedException("Only PLATFORM_ADMIN may delete organizations");
        }
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization", organizationId));
        if (assetLookupPort.hasAnyInOrganization(organizationId)) {
            throw new DomainException("Cannot delete organization '%s': it still has assets attached. Remove or reassign all assets before deleting."
                    .formatted(organization.getDisplayName()));
        }
        organizationRepository.deleteById(organizationId);
        auditPort.record(AuditPort.AuditEntry.of(caller.userId(), organizationId, "ORGANIZATION_DELETED",
                "Organization", organizationId, organization.getStatus().name(), null));
    }

    @Transactional(readOnly = true)
    public Organization getOrganization(TenantContext caller, String organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization", organizationId));
        caller.requireSameOrganization(organization.getId());
        return organization;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findDisplayName(String organizationId) {
        return organizationRepository.findById(organizationId).map(Organization::getDisplayName);
    }

    @Transactional(readOnly = true)
    public List<Organization> listVisibleOrganizations(TenantContext caller) {
        if (caller.isPlatformAdmin()) {
            return organizationRepository.findAll();
        }
        if (caller.organizationId() == null) {
            return List.of();
        }
        return organizationRepository.findById(caller.organizationId()).map(List::of).orElse(List.of());
    }
}
