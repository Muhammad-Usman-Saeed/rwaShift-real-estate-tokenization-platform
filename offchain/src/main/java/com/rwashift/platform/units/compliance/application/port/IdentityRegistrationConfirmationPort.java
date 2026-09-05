package com.rwashift.platform.units.compliance.application.port;

/**
 * Explicit cross-unit contract: Tokenization calls this once the on-chain {@code
 * registerInvestorIdentity} transaction actually confirms — never at submission time, since a
 * submitted transaction can still revert. Compliance only marks its {@code EligibilityDecision}
 * as identity-registered here, so a reverted registration is naturally retried on the next
 * eligibility evaluation instead of being permanently (and wrongly) considered done.
 */
public interface IdentityRegistrationConfirmationPort {

    void onIdentityRegistrationConfirmed(String investorId, String offeringId);
}
