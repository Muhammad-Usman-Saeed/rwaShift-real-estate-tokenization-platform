package com.rwashift.platform.units.kyc.api.dto;

import com.rwashift.platform.units.kyc.domain.model.KycCase;
import java.time.Instant;

public record KycCaseResponse(
        String id,
        String investorId,
        String status,
        String providerReference,
        Instant submittedAt,
        Instant reviewedAt,
        String reviewedBy,
        String rejectionReason
) {
    public static KycCaseResponse from(KycCase kycCase) {
        return new KycCaseResponse(
                kycCase.getId(), kycCase.getInvestorId(), kycCase.getStatus().name(), kycCase.getProviderReference(),
                kycCase.getSubmittedAt(), kycCase.getReviewedAt(), kycCase.getReviewedBy(), kycCase.getRejectionReason());
    }
}
