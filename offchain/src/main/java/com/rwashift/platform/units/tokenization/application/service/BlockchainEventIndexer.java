package com.rwashift.platform.units.tokenization.application.service;

import com.rwashift.platform.units.ownership.application.port.OwnershipProjectionPort;
import com.rwashift.platform.units.tokenization.domain.model.BlockchainEvent;
import com.rwashift.platform.units.tokenization.domain.model.ContractDeployment;
import com.rwashift.platform.units.tokenization.domain.model.ContractType;
import com.rwashift.platform.units.tokenization.domain.model.DeploymentStatus;
import com.rwashift.platform.units.tokenization.domain.model.IndexerCursor;
import com.rwashift.platform.units.tokenization.domain.repository.BlockchainEventRepository;
import com.rwashift.platform.units.tokenization.domain.repository.ContractDeploymentRepository;
import com.rwashift.platform.units.tokenization.domain.repository.IndexerCursorRepository;
import com.rwashift.platform.units.tokenization.infrastructure.integration.Web3jContractGateway;
import java.math.BigInteger;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;

/**
 * Polls every offering's confirmed T-REX token contract for {@code Transfer} events and updates
 * Ownership's read-model projection. Deliberately idempotent — each log is upserted keyed by
 * {@code (txHash, logIndex)} (the natural unique identity of "this exact event"; see
 * {@link BlockchainEvent}) so duplicate delivery, restart, or re-scanning an overlapping block
 * range never double-applies a transfer. Progress is persisted per contract in
 * {@link IndexerCursor} so a restart resumes rather than re-scanning from genesis or skipping
 * blocks produced while the process was down. A reverted transaction never reaches this indexer
 * in the first place — it never gets logs — so no special revert handling is needed here.
 */
@Service
class BlockchainEventIndexer {

    private static final Logger log = LoggerFactory.getLogger(BlockchainEventIndexer.class);
    private static final long MAX_BLOCK_RANGE_PER_POLL = 2000;

    private final ContractDeploymentRepository contractDeploymentRepository;
    private final IndexerCursorRepository cursorRepository;
    private final BlockchainEventRepository eventRepository;
    private final Web3jContractGateway gateway;
    private final OwnershipProjectionPort ownershipProjectionPort;

    BlockchainEventIndexer(ContractDeploymentRepository contractDeploymentRepository,
            IndexerCursorRepository cursorRepository, BlockchainEventRepository eventRepository,
            Web3jContractGateway gateway, OwnershipProjectionPort ownershipProjectionPort) {
        this.contractDeploymentRepository = contractDeploymentRepository;
        this.cursorRepository = cursorRepository;
        this.eventRepository = eventRepository;
        this.gateway = gateway;
        this.ownershipProjectionPort = ownershipProjectionPort;
    }

    @Scheduled(fixedDelayString = "${rwashift.blockchain.poll-interval-ms:4000}")
    @Transactional
    void indexAllTokens() {
        List<ContractDeployment> confirmedTokens =
                contractDeploymentRepository.findByContractTypeAndStatus(ContractType.TOKEN, DeploymentStatus.CONFIRMED);
        for (ContractDeployment deployment : confirmedTokens) {
            try {
                indexDeployment(deployment);
            } catch (Exception e) {
                log.warn("Error indexing token {} for offering {}", deployment.getContractAddress(), deployment.getOfferingId(), e);
            }
        }
    }

    private void indexDeployment(ContractDeployment deployment) {
        String tokenAddress = deployment.getContractAddress();
        long fromBlock = cursorRepository.findByContractAddress(tokenAddress)
                .map(IndexerCursor::getLastProcessedBlock)
                .orElse(deployment.getDeploymentBlock() != null ? deployment.getDeploymentBlock() : 0L) + 1;
        long latestBlock = gateway.currentBlockNumber();
        if (fromBlock > latestBlock) {
            return;
        }
        long toBlock = Math.min(latestBlock, fromBlock + MAX_BLOCK_RANGE_PER_POLL);

        List<EthLog.LogResult> logs = gateway.getTransferLogs(tokenAddress, fromBlock, toBlock);
        for (EthLog.LogResult<?> logResult : logs) {
            if (logResult.get() instanceof Log logEntry) {
                processLog(deployment, tokenAddress, logEntry);
            }
        }

        IndexerCursor cursor = cursorRepository.findByContractAddress(tokenAddress)
                .orElseGet(() -> new IndexerCursor(tokenAddress, 0));
        cursor.advanceTo(toBlock);
        cursorRepository.save(cursor);
    }

    private void processLog(ContractDeployment deployment, String tokenAddress, Log logEntry) {
        String txHash = logEntry.getTransactionHash();
        long logIndex = logEntry.getLogIndex().longValueExact();
        if (eventRepository.existsByTxHashAndLogIndex(txHash, logIndex)) {
            return;
        }
        String from = gateway.decodeTransferFrom(logEntry);
        String to = gateway.decodeTransferTo(logEntry);
        BigInteger amount = gateway.decodeTransferAmount(logEntry);
        long blockNumber = logEntry.getBlockNumber().longValueExact();

        eventRepository.save(new BlockchainEvent(tokenAddress, "Transfer", txHash, logIndex, blockNumber,
                "{\"from\":\"%s\",\"to\":\"%s\",\"amount\":\"%s\"}".formatted(from, to, amount)));

        ownershipProjectionPort.applyTransfer(tokenAddress, deployment.getOfferingId(), from, to, amount, blockNumber);
        log.debug("Indexed Transfer {} -> {} amount={} tx={} logIndex={}", from, to, amount, txHash, logIndex);
    }
}
