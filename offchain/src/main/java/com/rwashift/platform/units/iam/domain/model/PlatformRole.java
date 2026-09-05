package com.rwashift.platform.units.iam.domain.model;

/**
 * The platform's fixed role set. V1 keeps roles coarse-grained (role -> implied permission set,
 * enforced in method security / service-layer checks) rather than a fully dynamic
 * permission-assignment system; see ADR-009.
 */
public enum PlatformRole {
    PLATFORM_ADMIN,
    ORGANIZATION_ADMIN,
    ISSUER_ADMIN,
    ISSUER_OPERATOR,
    COMPLIANCE_OFFICER,
    INVESTOR
}
