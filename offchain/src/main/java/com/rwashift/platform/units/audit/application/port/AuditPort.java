package com.rwashift.platform.units.audit.application.port;

/**
 * Explicit cross-unit contract every other unit uses to append an audit record — the ONLY way
 * an {@code AuditLogEntry} ever gets created (see {@code AuditLogEntry}'s no-update/no-delete
 * guarantee). Mandatory call sites: KYC verification/rejection, eligibility changes, offering
 * approval, token deployment, identity registration, mint/burn/freeze/recovery, compliance
 * changes, payment confirmation, distribution approval.
 */
public interface AuditPort {

    void record(AuditEntry entry);

    record AuditEntry(
            String actorUserId,
            String organizationId,
            String action,
            String resourceType,
            String resourceId,
            String previousState,
            String newState,
            String correlationId,
            String blockchainTxHash
    ) {
        public static AuditEntry of(String actorUserId, String organizationId, String action, String resourceType,
                String resourceId, String previousState, String newState) {
            return new AuditEntry(actorUserId, organizationId, action, resourceType, resourceId, previousState,
                    newState, null, null);
        }
    }
}
