package com.rwashift.platform.units.tokenization.application.service;

import com.rwashift.platform.units.tokenization.infrastructure.configuration.BlockchainProperties;
import com.rwashift.platform.units.tokenization.infrastructure.integration.Web3jContractGateway;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.web3j.protocol.core.methods.response.Log;

/**
 * Backs the pre-login "connected network" panel (product spec follow-up: chain selection +
 * network insight on the login screen). Deliberately unauthenticated-safe: every field here is
 * public on-chain information (block numbers, timestamps, contract event names) with no investor
 * or transaction-amount data, so it's exposed via {@code NetworkStatusController} with no
 * {@code @PreAuthorize} — see {@code ResourceServerConfig}'s {@code /api/v1/public/**} matcher.
 *
 * <p>Every RPC call here is best-effort: a slow or unreachable node degrades the response (nulls,
 * an empty activity list) rather than throwing, since this panel renders before the user has even
 * signed in and must never block or break the login screen.
 */
@Service
public class NetworkStatusApplicationService {

    private static final Logger log = LoggerFactory.getLogger(NetworkStatusApplicationService.class);

    /** How many blocks back to sample for the average block-time estimate. */
    private static final long AVERAGE_BLOCK_TIME_SAMPLE = 10;
    /** How many blocks back to scan for platform contract events — bounded so eth_getLogs stays cheap. */
    private static final long ACTIVITY_LOOKBACK_BLOCKS = 2000;
    private static final int MAX_ACTIVITY_ITEMS = 8;

    private final Web3jContractGateway gateway;
    private final BlockchainProperties properties;

    public NetworkStatusApplicationService(Web3jContractGateway gateway, BlockchainProperties properties) {
        this.gateway = gateway;
        this.properties = properties;
    }

    public NetworkStatus getStatus() {
        long latestBlock;
        try {
            latestBlock = gateway.currentBlockNumber();
        } catch (Exception e) {
            log.debug("Network status: RPC unreachable", e);
            return NetworkStatus.unreachable(properties.getNetwork(), properties.getChainId());
        }

        return new NetworkStatus(true, properties.getNetwork(), properties.getChainId(), latestBlock,
                averageBlockTimeSeconds(latestBlock), recentActivity(latestBlock));
    }

    private Double averageBlockTimeSeconds(long latestBlock) {
        long sampleBlock = Math.max(0, latestBlock - AVERAGE_BLOCK_TIME_SAMPLE);
        if (sampleBlock == latestBlock) {
            return null;
        }
        try {
            Instant latestTimestamp = gateway.blockTimestamp(latestBlock);
            Instant sampleTimestamp = gateway.blockTimestamp(sampleBlock);
            if (latestTimestamp == null || sampleTimestamp == null) {
                return null;
            }
            long seconds = latestTimestamp.getEpochSecond() - sampleTimestamp.getEpochSecond();
            long blocks = latestBlock - sampleBlock;
            return blocks > 0 ? (double) seconds / blocks : null;
        } catch (Exception e) {
            log.debug("Network status: failed to sample block times for average block time", e);
            return null;
        }
    }

    private List<ChainActivityItem> recentActivity(long latestBlock) {
        try {
            long fromBlock = Math.max(0, latestBlock - ACTIVITY_LOOKBACK_BLOCKS);
            List<Log> logs = gateway.getRecentPlatformLogs(fromBlock, latestBlock);
            return logs.stream()
                    .sorted(Comparator.comparingLong((Log entry) -> entry.getBlockNumber().longValueExact())
                            .thenComparingLong(entry -> entry.getLogIndex().longValueExact())
                            .reversed())
                    .limit(MAX_ACTIVITY_ITEMS)
                    .map(this::toActivityItem)
                    .toList();
        } catch (Exception e) {
            log.debug("Network status: failed to fetch recent platform activity", e);
            return List.of();
        }
    }

    private ChainActivityItem toActivityItem(Log entry) {
        long blockNumber = entry.getBlockNumber().longValueExact();
        Instant blockTime;
        try {
            blockTime = gateway.blockTimestamp(blockNumber);
        } catch (Exception e) {
            blockTime = null;
        }
        return new ChainActivityItem(gateway.describePlatformEvent(entry), gateway.contractLabel(entry.getAddress()),
                blockNumber, entry.getTransactionHash(), blockTime);
    }

    public record NetworkStatus(boolean reachable, String network, long chainId, Long latestBlockNumber,
            Double averageBlockTimeSeconds, List<ChainActivityItem> recentActivity) {

        static NetworkStatus unreachable(String network, long chainId) {
            return new NetworkStatus(false, network, chainId, null, null, List.of());
        }
    }

    public record ChainActivityItem(String eventName, String contractLabel, long blockNumber, String txHash, Instant blockTime) {
    }
}
