package com.rwashift.platform.units.distribution.application.service;

import com.rwashift.platform.shared.domain.DomainException;
import com.rwashift.platform.shared.domain.NotFoundException;
import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.audit.application.port.AuditPort;
import com.rwashift.platform.units.distribution.application.command.CreateDistributionCommand;
import com.rwashift.platform.units.distribution.application.port.DistributionLookupPort;
import com.rwashift.platform.units.distribution.domain.model.Distribution;
import com.rwashift.platform.units.distribution.domain.model.DistributionEntitlement;
import com.rwashift.platform.units.distribution.domain.repository.DistributionEntitlementRepository;
import com.rwashift.platform.units.distribution.domain.repository.DistributionRepository;
import com.rwashift.platform.units.offering.application.port.OfferingLookupPort;
import com.rwashift.platform.units.ownership.application.port.OwnershipLookupPort;
import com.rwashift.platform.units.ownership.application.port.OwnershipSnapshot;
import com.rwashift.platform.units.tokenization.application.port.DistributionRecordingPort;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Distributions are calculated against the Ownership projection at calculation time (V1's
 * stand-in for a true record-date snapshot) and settled off-chain (simulated). Recording a
 * reference on-chain via {@link DistributionRecordingPort} is optional auditability, not the
 * settlement mechanism itself — see {@code units.tokenization}'s `RwaShiftDistributionRegistry`
 * integration.
 */
@Service
public class DistributionApplicationService implements DistributionLookupPort {

    private final DistributionRepository distributionRepository;
    private final DistributionEntitlementRepository entitlementRepository;
    private final OfferingLookupPort offeringLookupPort;
    private final OwnershipLookupPort ownershipLookupPort;
    private final DistributionRecordingPort distributionRecordingPort;
    private final AuditPort auditPort;

    public DistributionApplicationService(DistributionRepository distributionRepository,
            DistributionEntitlementRepository entitlementRepository, OfferingLookupPort offeringLookupPort,
            OwnershipLookupPort ownershipLookupPort, DistributionRecordingPort distributionRecordingPort,
            AuditPort auditPort) {
        this.distributionRepository = distributionRepository;
        this.entitlementRepository = entitlementRepository;
        this.offeringLookupPort = offeringLookupPort;
        this.ownershipLookupPort = ownershipLookupPort;
        this.distributionRecordingPort = distributionRecordingPort;
        this.auditPort = auditPort;
    }

    @Transactional
    public Distribution create(TenantContext caller, CreateDistributionCommand command) {
        offeringLookupPort.findSnapshot(command.offeringId())
                .filter(o -> o.organizationId().equals(caller.organizationId()))
                .orElseThrow(() -> new NotFoundException("Offering", command.offeringId()));
        Distribution distribution = new Distribution(caller.organizationId(), command.offeringId(),
                command.totalAmount(), command.currency(), command.recordDate());
        distribution = distributionRepository.save(distribution);
        auditPort.record(AuditPort.AuditEntry.of(caller.userId(), caller.organizationId(), "DISTRIBUTION_CREATED",
                "Distribution", distribution.getId(), null, distribution.getStatus().name()));
        return distribution;
    }

    @Transactional
    public Distribution calculate(TenantContext caller, String distributionId) {
        Distribution distribution = getOwned(caller, distributionId);
        String previousStatus = distribution.getStatus().name();
        List<OwnershipSnapshot> holdings = ownershipLookupPort.findByOffering(distribution.getOfferingId());
        BigInteger totalOutstanding = holdings.stream().map(OwnershipSnapshot::units).reduce(BigInteger.ZERO, BigInteger::add);
        if (totalOutstanding.equals(BigInteger.ZERO)) {
            throw new DomainException("Offering has no outstanding units to distribute against");
        }

        List<DistributionEntitlement> entitlements = holdings.stream()
                .filter(h -> h.units().compareTo(BigInteger.ZERO) > 0)
                .map(h -> {
                    BigDecimal share = new BigDecimal(h.units())
                            .divide(new BigDecimal(totalOutstanding), 12, RoundingMode.HALF_UP);
                    BigDecimal amount = distribution.getTotalAmount().multiply(share).setScale(4, RoundingMode.HALF_UP);
                    return new DistributionEntitlement(distribution.getId(), h.investorId(), h.walletAddress(), h.units(), amount);
                })
                .toList();
        entitlementRepository.saveAll(entitlements);

        distribution.markCalculated();
        Distribution saved = distributionRepository.save(distribution);
        auditPort.record(AuditPort.AuditEntry.of(caller.userId(), caller.organizationId(), "DISTRIBUTION_CALCULATED",
                "Distribution", distributionId, previousStatus, saved.getStatus().name()));
        return saved;
    }

    @Transactional
    public Distribution approve(TenantContext caller, String distributionId) {
        Distribution distribution = getOwned(caller, distributionId);
        distribution.approve();
        distribution = distributionRepository.save(distribution);
        auditPort.record(AuditPort.AuditEntry.of(caller.userId(), caller.organizationId(), "DISTRIBUTION_APPROVED",
                "Distribution", distributionId, "CALCULATED", distribution.getStatus().name()));

        byte[] metadataHash = sha256(distributionId + distribution.getOfferingId() + distribution.getTotalAmount());
        distributionRecordingPort.recordDistribution(distribution.getOfferingId(), distributionId,
                distribution.getRecordDate().getEpochSecond(), distribution.getTotalAmount().toBigInteger(), metadataHash);
        return distribution;
    }

    @Transactional
    public Distribution processSettlement(TenantContext caller, String distributionId) {
        Distribution distribution = getOwned(caller, distributionId);
        String previousStatus = distribution.getStatus().name();
        distribution.startProcessing();
        distribution = distributionRepository.save(distribution);
        auditPort.record(AuditPort.AuditEntry.of(caller.userId(), caller.organizationId(), "DISTRIBUTION_SETTLEMENT_STARTED",
                "Distribution", distributionId, previousStatus, distribution.getStatus().name()));
        return distribution;
    }

    /** V1 settlement is simulated — a platform/issuer admin marks it complete once "paid". */
    @Transactional
    public Distribution complete(TenantContext caller, String distributionId) {
        Distribution distribution = getOwned(caller, distributionId);
        String previousStatus = distribution.getStatus().name();
        distribution.complete();
        distribution = distributionRepository.save(distribution);
        auditPort.record(AuditPort.AuditEntry.of(caller.userId(), caller.organizationId(), "DISTRIBUTION_COMPLETED",
                "Distribution", distributionId, previousStatus, distribution.getStatus().name()));
        return distribution;
    }

    /** Investors may view any distribution (one they may hold an entitlement in); issuer-side callers only their own organization's. */
    @Transactional(readOnly = true)
    public Distribution getDistribution(TenantContext caller, String distributionId) {
        Distribution distribution = distributionRepository.findById(distributionId)
                .orElseThrow(() -> new NotFoundException("Distribution", distributionId));
        if (!caller.hasRole("INVESTOR")) {
            caller.requireSameOrganization(distribution.getOrganizationId());
        }
        return distribution;
    }

    @Transactional(readOnly = true)
    public List<DistributionEntitlement> getEntitlements(String distributionId) {
        return entitlementRepository.findByDistributionId(distributionId);
    }

    @Transactional(readOnly = true)
    public List<Distribution> listForOrganization(TenantContext caller) {
        return distributionRepository.findByOrganizationId(caller.organizationId());
    }

    /** For an investor browsing distributions on an offering they've invested in. */
    @Transactional(readOnly = true)
    public List<Distribution> listForOffering(String offeringId) {
        return distributionRepository.findByOfferingId(offeringId);
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<String> findOfferingId(String distributionId) {
        return distributionRepository.findById(distributionId).map(Distribution::getOfferingId);
    }

    private Distribution getOwned(TenantContext caller, String distributionId) {
        Distribution distribution = distributionRepository.findById(distributionId)
                .orElseThrow(() -> new NotFoundException("Distribution", distributionId));
        caller.requireSameOrganization(distribution.getOrganizationId());
        return distribution;
    }

    private static byte[] sha256(String input) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
