package com.rwashift.platform.units.kyc.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rwashift.platform.shared.domain.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

class KycCaseTest {

    @Test
    void happyPathToVerified() {
        KycCase kycCase = new KycCase("investor-1");
        kycCase.submit("provider-ref");
        kycCase.startReview();
        kycCase.verify("reviewer-1");

        assertThat(kycCase.getStatus()).isEqualTo(KycStatus.VERIFIED);
        assertThat(kycCase.getReviewedBy()).isEqualTo("reviewer-1");
    }

    @Test
    void rejectedCanBeResubmitted() {
        KycCase kycCase = new KycCase("investor-1");
        kycCase.submit("provider-ref");
        kycCase.startReview();
        kycCase.reject("reviewer-1", "documents unclear");
        assertThat(kycCase.getStatus()).isEqualTo(KycStatus.REJECTED);

        kycCase.submit("provider-ref-2");
        assertThat(kycCase.getStatus()).isEqualTo(KycStatus.SUBMITTED);
        assertThat(kycCase.getRejectionReason()).isNull();
    }

    @Test
    void verifyWithoutReviewFails() {
        KycCase kycCase = new KycCase("investor-1");
        kycCase.submit("provider-ref");
        assertThatThrownBy(() -> kycCase.verify("reviewer-1")).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void verifiedIsTerminal() {
        KycCase kycCase = new KycCase("investor-1");
        kycCase.submit("provider-ref");
        kycCase.startReview();
        kycCase.verify("reviewer-1");
        assertThatThrownBy(() -> kycCase.reject("reviewer-1", "too late")).isInstanceOf(InvalidStateTransitionException.class);
    }
}
