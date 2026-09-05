package com.rwashift.platform.units.tokenization.infrastructure.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;

/**
 * The only place in the platform that constructs a {@link Web3j} client or holds transaction-
 * signing material, per the "Tokenization is the only unit allowed to touch Web3j" rule.
 */
@Configuration
@EnableConfigurationProperties(BlockchainProperties.class)
public class Web3jConfig {

    private static final Logger log = LoggerFactory.getLogger(Web3jConfig.class);

    @Bean
    Web3j web3j(BlockchainProperties properties) {
        return Web3j.build(new HttpService(properties.getRpcUrl()));
    }

    @Bean
    Credentials agentCredentials(BlockchainProperties properties) {
        String key = properties.getAgentPrivateKey();
        if (key == null || key.isBlank()) {
            // Local/dev fallback only: never used when a real key is configured (Sepolia/prod).
            log.warn("BLOCKCHAIN_AGENT_PRIVATE_KEY not set — generating an ephemeral local signing key. "
                    + "This wallet must be pre-funded and pre-authorized as a TOKEN_AGENT/IDENTITY_AGENT on-chain to be usable.");
            try {
                ECKeyPair ephemeral = Keys.createEcKeyPair();
                return Credentials.create(ephemeral);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to generate ephemeral local signing key", e);
            }
        }
        return Credentials.create(key);
    }

    // Named explicitly (not just `transactionManager`) to avoid colliding with Spring's own
    // `transactionManager` bean (a `PlatformTransactionManager` for JPA) — different type, but
    // Spring's default bean-name-by-method-name would otherwise collide and fail context startup.
    @Bean
    TransactionManager web3jTransactionManager(Web3j web3j, Credentials agentCredentials, BlockchainProperties properties) {
        PollingTransactionReceiptProcessor receiptProcessor =
                new PollingTransactionReceiptProcessor(web3j, properties.getPollIntervalMs(), 40);
        return new RawTransactionManager(web3j, agentCredentials, properties.getChainId(), receiptProcessor);
    }
}
