package com.rwashift.platform.units.tokenization.application.service;

import com.rwashift.platform.units.investment.application.port.CryptoPaymentConfirmationPort;
import com.rwashift.platform.units.tokenization.domain.model.BlockchainEvent;
import com.rwashift.platform.units.tokenization.domain.model.IndexerCursor;
import com.rwashift.platform.units.tokenization.domain.repository.BlockchainEventRepository;
import com.rwashift.platform.units.tokenization.domain.repository.IndexerCursorRepository;
import com.rwashift.platform.units.tokenization.infrastructure.configuration.BlockchainProperties;
import com.rwashift.platform.units.tokenization.infrastructure.integration.Web3jContractGateway;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.core.methods.response.EthLog;
import org.web3j.protocol.core.methods.response.Log;

/**
 * Watches {@code RwaShiftPaymentRouter} for {@code PaymentReceived} events — investors pay
 * exclusively through that router's {@code payInvestment(investmentId, amount)}, which tags every
 * payment with the exact investment id it settles (see onchain/src/core/RwaShiftPaymentRouter.sol
 * and docs/critical-analysis.md #3 for why: a plain USDC transfer carries no reference field, so
 * an earlier version of this watcher had to guess which pending investment a payment was for by
 * matching on wallet + amount alone — ambiguous the moment one investor has two pending
 * investments of the same amount). Reading the id straight out of the event removes that
 * ambiguity entirely; {@link com.rwashift.platform.units.investment.application.service.InvestmentApplicationService#onCryptoPaymentDetected}
 * still re-validates status and amount before confirming, but no longer has to *guess* which
 * investment it's for.
 *
 * <p>Same block-range-sweep-with-persisted-cursor shape as {@link BlockchainEventIndexer}, reusing
 * {@link IndexerCursor} (keyed by the router's contract address) and {@link BlockchainEvent} for
 * idempotency (a payment this watcher already recorded is never re-dispatched, even across a
 * cursor-boundary restart).
 */
@Service
class CryptoPaymentWatcher {

    private static final Logger log = LoggerFactory.getLogger(CryptoPaymentWatcher.class);
    private static final long MAX_BLOCK_RANGE_PER_POLL = 2000;
    private static final int USDC_DECIMALS = 6;

    private final BlockchainProperties properties;
    private final Web3jContractGateway gateway;
    private final IndexerCursorRepository cursorRepository;
    private final BlockchainEventRepository eventRepository;
    private final CryptoPaymentConfirmationPort cryptoPaymentConfirmationPort;

    CryptoPaymentWatcher(BlockchainProperties properties, Web3jContractGateway gateway,
            IndexerCursorRepository cursorRepository, BlockchainEventRepository eventRepository,
            @Lazy CryptoPaymentConfirmationPort cryptoPaymentConfirmationPort) {
        this.properties = properties;
        this.gateway = gateway;
        this.cursorRepository = cursorRepository;
        this.eventRepository = eventRepository;
        this.cryptoPaymentConfirmationPort = cryptoPaymentConfirmationPort;
    }

    @Scheduled(fixedDelayString = "${rwashift.blockchain.poll-interval-ms:4000}")
    @Transactional
    void pollIncomingPayments() {
        String routerAddress = properties.getPaymentRouterAddress();
        if (routerAddress == null || routerAddress.isBlank()) {
            return; // Crypto payments not configured for this deployment yet — nothing to watch.
        }

        long fromBlock = cursorRepository.findByContractAddress(routerAddress)
                .map(IndexerCursor::getLastProcessedBlock)
                .orElse(0L) + 1;
        long latestBlock = gateway.currentBlockNumber();
        if (fromBlock > latestBlock) {
            return;
        }
        long toBlock = Math.min(latestBlock, fromBlock + MAX_BLOCK_RANGE_PER_POLL);

        List<EthLog.LogResult> logs = gateway.getPaymentReceivedLogs(routerAddress, fromBlock, toBlock);
        for (EthLog.LogResult<?> logResult : logs) {
            if (logResult.get() instanceof Log logEntry) {
                processLog(routerAddress, logEntry);
            }
        }

        IndexerCursor cursor = cursorRepository.findByContractAddress(routerAddress)
                .orElseGet(() -> new IndexerCursor(routerAddress, 0));
        cursor.advanceTo(toBlock);
        cursorRepository.save(cursor);
    }

    private void processLog(String routerAddress, Log logEntry) {
        String txHash = logEntry.getTransactionHash();
        long logIndex = logEntry.getLogIndex().longValueExact();
        if (eventRepository.existsByTxHashAndLogIndex(txHash, logIndex)) {
            return;
        }

        String payer = gateway.decodePaymentReceivedPayer(logEntry);
        Map.Entry<String, BigInteger> data = gateway.decodePaymentReceivedData(logEntry);
        String investmentId = data.getKey();
        BigInteger rawAmount = data.getValue();
        long blockNumber = logEntry.getBlockNumber().longValueExact();

        eventRepository.save(new BlockchainEvent(routerAddress, "PaymentReceived", txHash, logIndex, blockNumber,
                "{\"payer\":\"%s\",\"investmentId\":\"%s\",\"amount\":\"%s\"}".formatted(payer, investmentId, rawAmount)));

        BigDecimal amount = new BigDecimal(rawAmount).movePointLeft(USDC_DECIMALS);
        log.info("Crypto payment received: investmentId={} payer={} amount={} tx={}", investmentId, payer, amount, txHash);
        cryptoPaymentConfirmationPort.onCryptoPaymentDetected(investmentId, txHash, payer, amount);
    }
}
