package com.rwashift.platform.units.kyc.application.service;

import com.rwashift.platform.shared.domain.NotFoundException;
import com.rwashift.platform.units.audit.application.port.AuditPort;
import com.rwashift.platform.units.kyc.application.port.KycLookupPort;
import com.rwashift.platform.units.kyc.application.port.KycProvider;
import com.rwashift.platform.units.kyc.domain.model.KycCase;
import com.rwashift.platform.units.kyc.domain.model.KycStatus;
import com.rwashift.platform.units.kyc.domain.repository.KycCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KycApplicationService implements KycLookupPort {

    private final KycCaseRepository kycCaseRepository;
    private final KycProvider kycProvider;
    private final AuditPort auditPort;

    public KycApplicationService(KycCaseRepository kycCaseRepository, KycProvider kycProvider, AuditPort auditPort) {
        this.kycCaseRepository = kycCaseRepository;
        this.kycProvider = kycProvider;
        this.auditPort = auditPort;
    }

    @Transactional
    public KycCase submit(String investorId) {
        KycCase kycCase = kycCaseRepository.findByInvestorId(investorId).orElseGet(() -> new KycCase(investorId));
        String previousStatus = kycCase.getStatus().name();
        String providerReference = kycProvider.initiateVerification(investorId);
        kycCase.submit(providerReference);
        kycCase = kycCaseRepository.save(kycCase);
        auditPort.record(AuditPort.AuditEntry.of(investorId, null, "KYC_SUBMITTED", "KycCase", kycCase.getId(),
                previousStatus, kycCase.getStatus().name()));
        return kycCase;
    }

    @Transactional
    public KycCase startReview(String kycCaseId) {
        KycCase kycCase = getCase(kycCaseId);
        String previousStatus = kycCase.getStatus().name();
        kycCase.startReview();
        kycCase = kycCaseRepository.save(kycCase);
        auditPort.record(AuditPort.AuditEntry.of(null, null, "KYC_REVIEW_STARTED", "KycCase", kycCase.getId(),
                previousStatus, kycCase.getStatus().name()));
        return kycCase;
    }

    @Transactional
    public KycCase verify(String kycCaseId, String reviewerId) {
        KycCase kycCase = getCase(kycCaseId);
        String previousStatus = kycCase.getStatus().name();
        kycCase.verify(reviewerId);
        kycCase = kycCaseRepository.save(kycCase);
        auditPort.record(AuditPort.AuditEntry.of(reviewerId, null, "KYC_VERIFIED", "KycCase", kycCase.getId(),
                previousStatus, kycCase.getStatus().name()));
        return kycCase;
    }

    @Transactional
    public KycCase reject(String kycCaseId, String reviewerId, String reason) {
        KycCase kycCase = getCase(kycCaseId);
        String previousStatus = kycCase.getStatus().name();
        kycCase.reject(reviewerId, reason);
        kycCase = kycCaseRepository.save(kycCase);
        auditPort.record(AuditPort.AuditEntry.of(reviewerId, null, "KYC_REJECTED", "KycCase", kycCase.getId(),
                previousStatus, kycCase.getStatus().name()));
        return kycCase;
    }

    @Transactional(readOnly = true)
    public KycCase getByInvestor(String investorId) {
        return kycCaseRepository.findByInvestorId(investorId)
                .orElseThrow(() -> new NotFoundException("KycCase for investor", investorId));
    }

    /** Platform-wide KYC case list, for the compliance/admin review queue. */
    @Transactional(readOnly = true)
    public java.util.List<KycCase> listCases() {
        return kycCaseRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isVerified(String investorId) {
        return kycCaseRepository.findByInvestorId(investorId)
                .map(kycCase -> kycCase.getStatus() == KycStatus.VERIFIED)
                .orElse(false);
    }

    private KycCase getCase(String kycCaseId) {
        return kycCaseRepository.findById(kycCaseId).orElseThrow(() -> new NotFoundException("KycCase", kycCaseId));
    }
}
