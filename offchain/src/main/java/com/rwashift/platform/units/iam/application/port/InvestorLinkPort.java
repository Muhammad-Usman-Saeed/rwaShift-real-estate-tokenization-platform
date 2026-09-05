package com.rwashift.platform.units.iam.application.port;

/**
 * Explicit cross-unit contract: the Investor unit calls this after completing investor
 * onboarding for a platform user, so IAM can mint {@code investor_id}/{@code INVESTOR} claims
 * on future access tokens. This is the only way another unit is allowed to affect IAM state —
 * never through IAM's JPA repositories or entities directly.
 */
public interface InvestorLinkPort {

    void linkInvestor(String userId, String investorId);
}
