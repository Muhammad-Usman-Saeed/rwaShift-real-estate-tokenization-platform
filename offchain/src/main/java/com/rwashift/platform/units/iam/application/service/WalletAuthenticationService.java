package com.rwashift.platform.units.iam.application.service;

import com.rwashift.platform.shared.security.WalletIdentity;
import com.rwashift.platform.units.iam.domain.model.WalletLoginNonce;
import com.rwashift.platform.units.iam.domain.repository.WalletLoginNonceRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issues the EIP-4361-style ("Sign-In With Ethereum") message a wallet signs to prove ownership
 * of an address — see {@code WalletAuthenticationProvider} for how the resulting signature is
 * verified and turned into an authenticated session. Every field in the message (domain, nonce,
 * chain id) is protected implicitly: the signature covers the message byte-for-byte, so tampering
 * with any of them invalidates the signature rather than needing separate re-validation.
 */
@Service
public class WalletAuthenticationService {

    private static final Duration NONCE_TTL = Duration.ofMinutes(5);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter ISSUED_AT_FORMAT = DateTimeFormatter.ISO_INSTANT;

    private final WalletLoginNonceRepository nonceRepository;
    private final String frontendUrl;
    private final long chainId;

    public WalletAuthenticationService(WalletLoginNonceRepository nonceRepository,
            @Value("${rwashift.frontend-url}") String frontendUrl,
            @Value("${rwashift.blockchain.chain-id}") long chainId) {
        this.nonceRepository = nonceRepository;
        this.frontendUrl = frontendUrl;
        this.chainId = chainId;
    }

    @Transactional
    public String generateSignInMessage(String rawWalletAddress) {
        String address = WalletIdentity.normalize(rawWalletAddress);
        String nonce = generateNonce();
        nonceRepository.save(new WalletLoginNonce(address, nonce, Instant.now().plus(NONCE_TTL)));
        return buildMessage(address, nonce);
    }

    private String buildMessage(String address, String nonce) {
        return """
                rwashift.com wants you to sign in with your Ethereum account:
                %s

                Sign in to rwaShift Real Estate.

                URI: %s
                Version: 1
                Chain ID: %d
                Nonce: %s
                Issued At: %s"""
                .formatted(address, frontendUrl, chainId, nonce, ISSUED_AT_FORMAT.format(Instant.now()));
    }

    private static String generateNonce() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        StringBuilder hex = new StringBuilder(32);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
