package com.rwashift.platform.units.audit.api.dto;

import com.rwashift.platform.units.audit.domain.model.AuditLogEntry;
import java.time.Instant;

public record AuditLogEntryResponse(
        String id,
        String actorUserId,
        String organizationId,
        String action,
        String resourceType,
        String resourceId,
        String previousState,
        String newState,
        String correlationId,
        String blockchainTxHash,
        Instant occurredAt
) {
    public static AuditLogEntryResponse from(AuditLogEntry e) {
        return new AuditLogEntryResponse(e.getId(), e.getActorUserId(), e.getOrganizationId(), e.getAction(),
                e.getResourceType(), e.getResourceId(), e.getPreviousState(), e.getNewState(), e.getCorrelationId(),
                e.getBlockchainTxHash(), e.getOccurredAt());
    }
}
