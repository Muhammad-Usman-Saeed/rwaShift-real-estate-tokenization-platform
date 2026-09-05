package com.rwashift.platform.units.offering.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rwashift.platform.shared.domain.DomainException;
import com.rwashift.platform.shared.domain.InvalidStateTransitionException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OfferingTest {

    private Offering newOffering() {
        return new Offering("org-1", "asset-1", "legal-1", "Demo Offering", new BigDecimal("2000000.00"), "USD",
                20_000, new BigDecimal("100.00"), new BigDecimal("5000.00"), new BigDecimal("20.0000"),
                Instant.now(), Instant.now().plusSeconds(3600));
    }

    @Test
    void approveWithoutSubmittingForReviewFails() {
        Offering offering = newOffering();
        assertThatThrownBy(offering::approve).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void happyPathToOpen() {
        Offering offering = newOffering();
        offering.submitForReview();
        offering.approve();
        offering.beginTokenization();
        offering.openForInvestment();
        assertThat(offering.getStatus()).isEqualTo(OfferingStatus.OPEN);
    }

    @Test
    void recordUnitsIssuedBeyondCapacityThrows() {
        Offering offering = newOffering();
        assertThatThrownBy(() -> offering.recordUnitsIssued(20_001)).isInstanceOf(DomainException.class);
    }

    @Test
    void recordUnitsIssuedToFullCapacityMarksFundedWhenOpen() {
        Offering offering = newOffering();
        offering.submitForReview();
        offering.approve();
        offering.beginTokenization();
        offering.openForInvestment();

        offering.recordUnitsIssued(20_000);

        assertThat(offering.getStatus()).isEqualTo(OfferingStatus.FUNDED);
        assertThat(offering.getUnitsAvailable()).isZero();
    }
}
