package com.rwashift.platform.units.offering.application.service;

import com.rwashift.platform.shared.domain.DomainException;
import com.rwashift.platform.shared.domain.NotFoundException;
import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.audit.application.port.AuditPort;
import com.rwashift.platform.units.asset.application.port.AssetLookupPort;
import com.rwashift.platform.units.legalstructure.application.port.LegalStructureLookupPort;
import com.rwashift.platform.units.offering.application.command.CreateOfferingCommand;
import com.rwashift.platform.units.offering.application.command.UpdateOfferingCommand;
import com.rwashift.platform.units.offering.application.port.OfferingLookupPort;
import com.rwashift.platform.units.offering.application.port.OfferingSnapshot;
import com.rwashift.platform.units.offering.application.port.OfferingTokenizationCallbackPort;
import com.rwashift.platform.units.offering.domain.model.Offering;
import com.rwashift.platform.units.offering.domain.model.OfferingStatus;
import com.rwashift.platform.units.offering.domain.repository.OfferingRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OfferingApplicationService implements OfferingLookupPort, OfferingTokenizationCallbackPort {

    private final OfferingRepository offeringRepository;
    private final AssetLookupPort assetLookupPort;
    private final LegalStructureLookupPort legalStructureLookupPort;
    private final AuditPort auditPort;

    public OfferingApplicationService(OfferingRepository offeringRepository, AssetLookupPort assetLookupPort,
            LegalStructureLookupPort legalStructureLookupPort, AuditPort auditPort) {
        this.offeringRepository = offeringRepository;
        this.assetLookupPort = assetLookupPort;
        this.legalStructureLookupPort = legalStructureLookupPort;
        this.auditPort = auditPort;
    }

    @Transactional
    public Offering createOffering(TenantContext caller, CreateOfferingCommand command) {
        String organizationId = caller.organizationId();
        if (!assetLookupPort.existsInOrganization(command.assetId(), organizationId)) {
            throw new DomainException("Asset '%s' does not belong to this organization".formatted(command.assetId()));
        }
        if (!legalStructureLookupPort.existsInOrganization(command.legalStructureId(), organizationId)) {
            throw new DomainException("Legal structure '%s' does not belong to this organization".formatted(command.legalStructureId()));
        }
        Offering offering = new Offering(organizationId, command.assetId(), command.legalStructureId(), command.name(),
                command.targetRaise(), command.currency(), command.totalUnits(), command.unitPrice(),
                command.minimumInvestment(), command.offeredInterestPercentage(), command.openingDate(), command.closingDate());
        offering = offeringRepository.save(offering);
        auditPort.record(AuditPort.AuditEntry.of(caller.userId(), organizationId, "OFFERING_CREATED",
                "Offering", offering.getId(), null, offering.getStatus().name()));
        return offering;
    }

    /** Editable only while DRAFT — the creator (or org admin) can fix mistakes, but not rewrite terms once submitted for review. */
    @Transactional
    public Offering updateOffering(TenantContext caller, String offeringId, UpdateOfferingCommand command) {
        Offering offering = getOwned(caller, offeringId);
        if (offering.getStatus() != OfferingStatus.DRAFT) {
            throw new DomainException("Cannot edit offering '%s': it has already been submitted for review.".formatted(offering.getName()));
        }
        offering.update(command.name(), command.targetRaise(), command.currency(), command.totalUnits(), command.unitPrice(),
                command.minimumInvestment(), command.offeredInterestPercentage(), command.openingDate(), command.closingDate());
        offering = offeringRepository.save(offering);
        auditPort.record(AuditPort.AuditEntry.of(caller.userId(), caller.organizationId(), "OFFERING_UPDATED",
                "Offering", offeringId, null, offering.getStatus().name()));
        return offering;
    }

    @Transactional
    public Offering submitForReview(TenantContext caller, String offeringId) {
        return transition(caller, offeringId, Offering::submitForReview, "OFFERING_SUBMITTED_FOR_REVIEW");
    }

    @Transactional
    public Offering approve(TenantContext caller, String offeringId) {
        return transition(caller, offeringId, Offering::approve, "OFFERING_APPROVED");
    }

    @Transactional
    public Offering reject(TenantContext caller, String offeringId) {
        return transition(caller, offeringId, Offering::reject, "OFFERING_REJECTED");
    }

    @Transactional
    public Offering beginTokenization(TenantContext caller, String offeringId) {
        return transition(caller, offeringId, Offering::beginTokenization, "OFFERING_TOKENIZATION_STARTED");
    }

    /**
     * System-triggered counterpart of {@link #beginTokenization(TenantContext, String)} — called
     * by Tokenization when it starts deploying, not by a user action. Idempotent when the
     * offering is already TOKENIZING (Tokenization is the only real caller today, and it always
     * drives this transition itself rather than requiring callers to hit the user-facing
     * begin-tokenization endpoint first — see {@code TokenizationApplicationService.deployOfferingToken}).
     */
    @Override
    @Transactional
    public void beginTokenization(String offeringId) {
        Offering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new NotFoundException("Offering", offeringId));
        if (offering.getStatus() == OfferingStatus.TOKENIZING) {
            return;
        }
        String previousStatus = offering.getStatus().name();
        offering.beginTokenization();
        offering = offeringRepository.save(offering);
        auditPort.record(AuditPort.AuditEntry.of(null, offering.getOrganizationId(), "OFFERING_TOKENIZATION_STARTED",
                "Offering", offeringId, previousStatus, offering.getStatus().name()));
    }

    /** Called by Tokenization once the on-chain token suite is deployed and confirmed. */
    @Transactional
    public Offering openForInvestment(String offeringId) {
        Offering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new NotFoundException("Offering", offeringId));
        String previousStatus = offering.getStatus().name();
        offering.openForInvestment();
        offering = offeringRepository.save(offering);
        auditPort.record(AuditPort.AuditEntry.of(null, offering.getOrganizationId(), "OFFERING_OPENED_FOR_INVESTMENT",
                "Offering", offeringId, previousStatus, offering.getStatus().name()));
        return offering;
    }

    @Override
    @Transactional
    public void onTokenizationConfirmed(String offeringId) {
        openForInvestment(offeringId);
    }

    @Transactional
    public Offering close(TenantContext caller, String offeringId) {
        return transition(caller, offeringId, Offering::close, "OFFERING_CLOSED");
    }

    /** Investors may view any offering (the marketplace is cross-org by nature); issuer-side callers only their own. */
    @Transactional(readOnly = true)
    public Offering getOffering(TenantContext caller, String offeringId) {
        Offering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new NotFoundException("Offering", offeringId));
        if (!caller.hasRole("INVESTOR")) {
            caller.requireSameOrganization(offering.getOrganizationId());
        }
        return offering;
    }

    /**
     * Platform admins see every organization's offerings (needed for the approval queue);
     * investors — who have no organization of their own — browse the cross-org marketplace of
     * currently open offerings (the "Investment Opportunities" screen); issuer-side callers see
     * only their own organization's.
     */
    @Transactional(readOnly = true)
    public List<Offering> listOfferings(TenantContext caller) {
        if (caller.isPlatformAdmin()) {
            return offeringRepository.findAll();
        }
        if (caller.hasRole("INVESTOR")) {
            return offeringRepository.findAll().stream().filter(o -> o.getStatus() == OfferingStatus.OPEN).toList();
        }
        return offeringRepository.findByOrganizationId(caller.organizationId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OfferingSnapshot> findSnapshot(String offeringId) {
        return offeringRepository.findById(offeringId).map(o -> new OfferingSnapshot(
                o.getId(), o.getOrganizationId(), o.getLegalStructureId(), o.getName(), o.getCurrency(), o.getUnitPrice(),
                o.getMinimumInvestment(), o.getTotalUnits(), o.getUnitsAvailable(), o.getStatus() == OfferingStatus.OPEN));
    }

    /**
     * Called once per settled investment (see Investment's {@code onTokenIssuanceConfirmed}),
     * which already records its own {@code TOKEN_ISSUANCE_CONFIRMED} audit entry against the
     * Investment resource — deliberately not duplicated here per-offering to avoid a redundant
     * audit entry for every single investment.
     */
    @Override
    @Transactional
    public void recordUnitsIssued(String offeringId, long units) {
        Offering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new NotFoundException("Offering", offeringId));
        offering.recordUnitsIssued(units);
        offeringRepository.save(offering);
    }

    private Offering transition(TenantContext caller, String offeringId, java.util.function.Consumer<Offering> transition,
            String action) {
        Offering offering = getOwned(caller, offeringId);
        String previousStatus = offering.getStatus().name();
        transition.accept(offering);
        offering = offeringRepository.save(offering);
        auditPort.record(AuditPort.AuditEntry.of(caller.userId(), caller.organizationId(), action,
                "Offering", offeringId, previousStatus, offering.getStatus().name()));
        return offering;
    }

    /** Used only by mutating transitions (submit/approve/reject/begin-tokenization/close) — always issuer/admin-owned, never investor-accessible. */
    private Offering getOwned(TenantContext caller, String offeringId) {
        Offering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new NotFoundException("Offering", offeringId));
        caller.requireSameOrganization(offering.getOrganizationId());
        return offering;
    }
}
