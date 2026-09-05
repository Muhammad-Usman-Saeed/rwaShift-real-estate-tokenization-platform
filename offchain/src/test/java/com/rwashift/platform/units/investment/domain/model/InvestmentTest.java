package com.rwashift.platform.units.investment.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rwashift.platform.shared.domain.InvalidStateTransitionException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class InvestmentTest {

    private Investment newInvestment() {
        return new Investment("org-1", "investor-1", "offering-1", new BigDecimal("10000.00"), "USD",
                new BigDecimal("100.00"), 100, "idem-key-1");
    }

    @Test
    void happyPathToSettled() {
        Investment investment = newInvestment();
        investment.moveToEligibilityPending();
        investment.moveToPaymentPending();
        investment.confirmPayment();
        investment.requestTokenIssuance();
        investment.settleTokenIssuance();

        assertThat(investment.getStatus()).isEqualTo(InvestmentStatus.SETTLED);
        assertThat(investment.getPaymentStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(investment.getTokenIssuanceStatus()).isEqualTo(TokenIssuanceStatus.CONFIRMED);
    }

    @Test
    void paymentFailureIsTerminal() {
        Investment investment = newInvestment();
        investment.moveToEligibilityPending();
        investment.moveToPaymentPending();
        investment.failPayment("card declined");

        assertThat(investment.getStatus()).isEqualTo(InvestmentStatus.PAYMENT_FAILED);
        assertThatThrownBy(investment::confirmPayment).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void tokenIssuanceCanFailAfterPaymentConfirmed() {
        Investment investment = newInvestment();
        investment.moveToEligibilityPending();
        investment.moveToPaymentPending();
        investment.confirmPayment();
        investment.requestTokenIssuance();
        investment.failTokenIssuance("chain reverted");

        assertThat(investment.getStatus()).isEqualTo(InvestmentStatus.TOKEN_ISSUANCE_FAILED);
        assertThat(investment.getTokenIssuanceStatus()).isEqualTo(TokenIssuanceStatus.FAILED);
    }

    @Test
    void cannotSkipStates() {
        Investment investment = newInvestment();
        assertThatThrownBy(investment::confirmPayment).isInstanceOf(InvalidStateTransitionException.class);
    }
}
