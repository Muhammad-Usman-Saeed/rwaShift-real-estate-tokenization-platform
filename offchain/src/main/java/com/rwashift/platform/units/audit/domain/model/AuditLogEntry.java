package com.rwashift.platform.units.audit.domain.model;

import com.rwashift.platform.shared.domain.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * One append-only audit record. There is deliberately no update or delete path anywhere in this
 * unit — not in the domain model, not in the repository port, not in the REST API — because an
 * editable audit log is not an audit log. Mandatory audit points (KYC verification, eligibility
 * change, offering approval, token deployment, identity registration, mint, burn, freeze,
 * recovery, compliance change, payment confirmation, distribution approval) call
 * {@code AuditApplicationService.record} directly; nothing here infers "what happened" from
 * other units' state.
 */
@Entity
@Table(name = "audit_log_entry")
public class AuditLogEntry {

    @Id
    @Column(name = "id", length = 26, nullable = false, updatable = false)
    private String id = IdGenerator.newId();

    @Column(name = "actor_user_id", length = 26)
    private String actorUserId;

    @Column(name = "organization_id", length = 26)
    private String organizationId;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 64)
    private String resourceType;

    @Column(name = "resource_id", nullable = false, length = 60)
    private String resourceId;

    @Column(name = "previous_state", length = 100)
    private String previousState;

    @Column(name = "new_state", length = 100)
    private String newState;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "blockchain_tx_hash", length = 66)
    private String blockchainTxHash;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt = Instant.now();

    protected AuditLogEntry() {
    }

    public AuditLogEntry(String actorUserId, String organizationId, String action, String resourceType,
            String resourceId, String previousState, String newState, String correlationId, String ipAddress,
            String blockchainTxHash) {
        this.actorUserId = actorUserId;
        this.organizationId = organizationId;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.previousState = previousState;
        this.newState = newState;
        this.correlationId = correlationId;
        this.ipAddress = ipAddress;
        this.blockchainTxHash = blockchainTxHash;
    }

    public String getId() {
        return id;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public String getAction() {
        return action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getPreviousState() {
        return previousState;
    }

    public String getNewState() {
        return newState;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getBlockchainTxHash() {
        return blockchainTxHash;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
