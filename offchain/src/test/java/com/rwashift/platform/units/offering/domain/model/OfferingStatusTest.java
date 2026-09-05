package com.rwashift.platform.units.offering.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OfferingStatusTest {

    @Test
    void draftOnlyAllowsUnderReview() {
        assertThat(OfferingStatus.DRAFT.canTransitionTo(OfferingStatus.UNDER_REVIEW)).isTrue();
        assertThat(OfferingStatus.DRAFT.canTransitionTo(OfferingStatus.APPROVED)).isFalse();
        assertThat(OfferingStatus.DRAFT.canTransitionTo(OfferingStatus.OPEN)).isFalse();
    }

    @Test
    void underReviewAllowsApproveOrReject() {
        assertThat(OfferingStatus.UNDER_REVIEW.canTransitionTo(OfferingStatus.APPROVED)).isTrue();
        assertThat(OfferingStatus.UNDER_REVIEW.canTransitionTo(OfferingStatus.REJECTED)).isTrue();
        assertThat(OfferingStatus.UNDER_REVIEW.canTransitionTo(OfferingStatus.TOKENIZING)).isFalse();
    }

    @Test
    void openAllowsFundedOrEarlyClose() {
        assertThat(OfferingStatus.OPEN.canTransitionTo(OfferingStatus.FUNDED)).isTrue();
        assertThat(OfferingStatus.OPEN.canTransitionTo(OfferingStatus.CLOSED)).isTrue();
    }

    @Test
    void terminalStatesAllowNoTransitions() {
        for (OfferingStatus target : OfferingStatus.values()) {
            assertThat(OfferingStatus.CLOSED.canTransitionTo(target)).isFalse();
            assertThat(OfferingStatus.REJECTED.canTransitionTo(target)).isFalse();
        }
    }
}
