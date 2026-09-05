package com.rwashift.platform.units.tokenization.domain.model;

import com.rwashift.platform.shared.domain.IdGenerator;
import com.rwashift.platform.shared.domain.InvalidStateTransitionException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Every write this platform makes to the blockchain is tracked here. Submission is never
 * treated as business completion: callers only find out a transaction succeeded once this
 * record reaches {@link BlockchainTransactionStatus#CONFIRMED} (see
 * {@code BlockchainTransactionManager}'s confirmation poller) — see ADR-010.
 */
@Entity
@Table(name = "blockchain_transaction")
public class BlockchainTransaction {

    @Id
    @Column(name = "id", length = 26, nullable = false, updatable = false)
    private String id = IdGenerator.newId();

    @Enumerated(EnumType.STRING)
    @Column(name = "business_reference_type", nullable = false, length = 32)
    private BusinessReferenceType businessReferenceType;

    @Column(name = "business_reference_id", nullable = false, length = 26)
    private String businessReferenceId;

    @Column(name = "network", nullable = false, length = 32)
    private String network;

    @Column(name = "chain_id", nullable = false)
    private long chainId;

    @Column(name = "contract_address", length = 42)
    private String contractAddress;

    @Column(name = "method", nullable = false, length = 100)
    private String method;

    @Column(name = "tx_hash", length = 66)
    private String txHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private BlockchainTransactionStatus status = BlockchainTransactionStatus.CREATED;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "block_number")
    private Long blockNumber;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    protected BlockchainTransaction() {
    }

    public BlockchainTransaction(BusinessReferenceType businessReferenceType, String businessReferenceId,
            String network, long chainId, String contractAddress, String method) {
        this.businessReferenceType = businessReferenceType;
        this.businessReferenceId = businessReferenceId;
        this.network = network;
        this.chainId = chainId;
        this.contractAddress = contractAddress;
        this.method = method;
    }

    public void markSubmitted(String txHash) {
        transitionTo(BlockchainTransactionStatus.SUBMITTED);
        this.txHash = txHash;
        this.submittedAt = Instant.now();
    }

    public void markPending() {
        transitionTo(BlockchainTransactionStatus.PENDING);
    }

    public void markConfirmed(long blockNumber) {
        transitionTo(BlockchainTransactionStatus.CONFIRMED);
        this.blockNumber = blockNumber;
        this.confirmedAt = Instant.now();
    }

    public void markFailed(String reason) {
        this.status = BlockchainTransactionStatus.FAILED;
        this.failureReason = reason;
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    private void transitionTo(BlockchainTransactionStatus target) {
        boolean allowed = switch (status) {
            case CREATED -> target == BlockchainTransactionStatus.SUBMITTED;
            case SUBMITTED -> target == BlockchainTransactionStatus.PENDING || target == BlockchainTransactionStatus.CONFIRMED;
            case PENDING -> target == BlockchainTransactionStatus.CONFIRMED;
            case CONFIRMED, FAILED -> false;
        };
        if (!allowed) {
            throw new InvalidStateTransitionException("BlockchainTransaction", status.name(), target.name());
        }
        this.status = target;
    }

    public String getId() {
        return id;
    }

    public BusinessReferenceType getBusinessReferenceType() {
        return businessReferenceType;
    }

    public String getBusinessReferenceId() {
        return businessReferenceId;
    }

    public String getNetwork() {
        return network;
    }

    public long getChainId() {
        return chainId;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public String getMethod() {
        return method;
    }

    public String getTxHash() {
        return txHash;
    }

    public BlockchainTransactionStatus getStatus() {
        return status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public Long getBlockNumber() {
        return blockNumber;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public int getRetryCount() {
        return retryCount;
    }
}
