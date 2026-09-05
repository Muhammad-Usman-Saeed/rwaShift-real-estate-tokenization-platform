package com.rwashift.platform.units.legalstructure.application.service;

import com.rwashift.platform.shared.domain.DomainException;
import com.rwashift.platform.shared.domain.NotFoundException;
import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.asset.application.port.AssetLookupPort;
import com.rwashift.platform.units.audit.application.port.AuditPort;
import com.rwashift.platform.units.legalstructure.application.command.CreateLegalStructureCommand;
import com.rwashift.platform.units.legalstructure.application.command.UpdateLegalStructureCommand;
import com.rwashift.platform.units.legalstructure.application.port.LegalStructureLookupPort;
import com.rwashift.platform.units.legalstructure.domain.model.LegalStructure;
import com.rwashift.platform.units.legalstructure.domain.model.LegalStructureStatus;
import com.rwashift.platform.units.legalstructure.domain.repository.LegalStructureRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LegalStructureApplicationService implements LegalStructureLookupPort {

    private final LegalStructureRepository legalStructureRepository;
    private final AssetLookupPort assetLookupPort;
    private final AuditPort auditPort;

    public LegalStructureApplicationService(LegalStructureRepository legalStructureRepository,
            AssetLookupPort assetLookupPort, AuditPort auditPort) {
        this.legalStructureRepository = legalStructureRepository;
        this.assetLookupPort = assetLookupPort;
        this.auditPort = auditPort;
    }

    @Transactional
    public LegalStructure createLegalStructure(TenantContext caller, CreateLegalStructureCommand command) {
        if (!assetLookupPort.existsInOrganization(command.assetId(), caller.organizationId())) {
            throw new DomainException("Asset '%s' does not belong to this organization".formatted(command.assetId()));
        }
        LegalStructure legalStructure = new LegalStructure(caller.organizationId(), command.assetId(),
                command.legalEntityName(), command.entityType(), command.jurisdiction(), command.registrationNumber(),
                command.relationshipToAsset(), command.investmentInstrumentType());
        legalStructure = legalStructureRepository.save(legalStructure);
        auditPort.record(AuditPort.AuditEntry.of(caller.userId(), caller.organizationId(), "LEGAL_STRUCTURE_CREATED",
                "LegalStructure", legalStructure.getId(), null, legalStructure.getStatus().name()));
        return legalStructure;
    }

    /** Editable only while DRAFT — the creator (or org admin) can fix mistakes, but not rewrite a structure that's already been activated. */
    @Transactional
    public LegalStructure updateLegalStructure(TenantContext caller, String legalStructureId, UpdateLegalStructureCommand command) {
        LegalStructure legalStructure = getOwned(caller, legalStructureId);
        if (legalStructure.getStatus() != LegalStructureStatus.DRAFT) {
            throw new DomainException("Cannot edit legal structure '%s': it has already been activated."
                    .formatted(legalStructure.getLegalEntityName()));
        }
        legalStructure.update(command.legalEntityName(), command.entityType(), command.jurisdiction(),
                command.registrationNumber(), command.relationshipToAsset(), command.investmentInstrumentType());
        legalStructure = legalStructureRepository.save(legalStructure);
        auditPort.record(AuditPort.AuditEntry.of(caller.userId(), caller.organizationId(), "LEGAL_STRUCTURE_UPDATED",
                "LegalStructure", legalStructureId, null, legalStructure.getStatus().name()));
        return legalStructure;
    }

    @Transactional
    public LegalStructure activate(TenantContext caller, String legalStructureId) {
        LegalStructure legalStructure = getOwned(caller, legalStructureId);
        String previousStatus = legalStructure.getStatus().name();
        legalStructure.activate();
        legalStructure = legalStructureRepository.save(legalStructure);
        auditPort.record(AuditPort.AuditEntry.of(caller.userId(), caller.organizationId(), "LEGAL_STRUCTURE_ACTIVATED",
                "LegalStructure", legalStructureId, previousStatus, legalStructure.getStatus().name()));
        return legalStructure;
    }

    /** Investors may view any legal structure (backing an offering they might invest in); issuer-side callers only their own. */
    @Transactional(readOnly = true)
    public LegalStructure getLegalStructure(TenantContext caller, String legalStructureId) {
        LegalStructure legalStructure = legalStructureRepository.findById(legalStructureId)
                .orElseThrow(() -> new NotFoundException("LegalStructure", legalStructureId));
        if (!caller.hasRole("INVESTOR")) {
            caller.requireSameOrganization(legalStructure.getOrganizationId());
        }
        return legalStructure;
    }

    @Transactional(readOnly = true)
    public List<LegalStructure> listLegalStructures(TenantContext caller) {
        return caller.isPlatformAdmin()
                ? legalStructureRepository.findAll()
                : legalStructureRepository.findByOrganizationId(caller.organizationId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInOrganization(String legalStructureId, String organizationId) {
        return legalStructureRepository.existsByIdAndOrganizationId(legalStructureId, organizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<String> findDisplayName(String legalStructureId) {
        return legalStructureRepository.findById(legalStructureId).map(LegalStructure::getLegalEntityName);
    }

    private LegalStructure getOwned(TenantContext caller, String legalStructureId) {
        LegalStructure legalStructure = legalStructureRepository.findById(legalStructureId)
                .orElseThrow(() -> new NotFoundException("LegalStructure", legalStructureId));
        caller.requireSameOrganization(legalStructure.getOrganizationId());
        return legalStructure;
    }
}
