package com.rwashift.platform.units.tokenization.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Tracks the last block successfully indexed per contract, so a restart resumes from where it
 * left off instead of re-scanning the whole chain or silently skipping blocks produced while
 * the indexer was down.
 */
@Entity
@Table(name = "indexer_cursor")
public class IndexerCursor {

    @Id
    @Column(name = "contract_address", length = 42, nullable = false)
    private String contractAddress;

    @Column(name = "last_processed_block", nullable = false)
    private long lastProcessedBlock;

    protected IndexerCursor() {
    }

    public IndexerCursor(String contractAddress, long lastProcessedBlock) {
        this.contractAddress = contractAddress;
        this.lastProcessedBlock = lastProcessedBlock;
    }

    public void advanceTo(long blockNumber) {
        this.lastProcessedBlock = blockNumber;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public long getLastProcessedBlock() {
        return lastProcessedBlock;
    }
}
