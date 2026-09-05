package com.rwashift.platform.units.tokenization.domain.model;

import com.rwashift.platform.shared.domain.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * One indexed on-chain log entry. {@code (tx_hash, log_index)} is the natural unique key for
 * "this exact event", which is what makes re-processing (duplicate delivery, indexer restart,
 * chain reorg replay) idempotent — the indexer always upserts on that pair rather than
 * assuming it has never seen a block before (see ADR-010 / README event-indexing notes).
 */
@Entity
@Table(name = "blockchain_event", uniqueConstraints = @UniqueConstraint(columnNames = {"tx_hash", "log_index"}))
public class BlockchainEvent {

    @Id
    @Column(name = "id", length = 26, nullable = false, updatable = false)
    private String id = IdGenerator.newId();

    @Column(name = "contract_address", nullable = false, length = 42)
    private String contractAddress;

    @Column(name = "event_name", nullable = false, length = 100)
    private String eventName;

    @Column(name = "tx_hash", nullable = false, length = 66)
    private String txHash;

    @Column(name = "log_index", nullable = false)
    private long logIndex;

    @Column(name = "block_number", nullable = false)
    private long blockNumber;

    @Column(name = "payload", length = 4000)
    private String payload;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt = Instant.now();

    protected BlockchainEvent() {
    }

    public BlockchainEvent(String contractAddress, String eventName, String txHash, long logIndex, long blockNumber,
            String payload) {
        this.contractAddress = contractAddress;
        this.eventName = eventName;
        this.txHash = txHash;
        this.logIndex = logIndex;
        this.blockNumber = blockNumber;
        this.payload = payload;
    }

    public String getId() {
        return id;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public String getEventName() {
        return eventName;
    }

    public String getTxHash() {
        return txHash;
    }

    public long getLogIndex() {
        return logIndex;
    }

    public long getBlockNumber() {
        return blockNumber;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
