package com.rwashift.platform.units.tokenization.infrastructure.security;

import com.rwashift.platform.units.tokenization.infrastructure.configuration.BlockchainProperties;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Encrypts/decrypts per-organization on-chain private keys at rest (AES-256-GCM, random 12-byte
 * IV prepended to the ciphertext, base64-encoded for storage in {@code OrganizationWallet}). The
 * master key ({@code rwashift.blockchain.org-wallet-master-key} / {@code ORG_WALLET_MASTER_KEY})
 * never itself touches the database — it's a container env var, same trust tier as {@code
 * BLOCKCHAIN_AGENT_PRIVATE_KEY}. Deliberately fails startup rather than falling back to an
 * ephemeral key: unlike the platform agent key (which can regenerate freely since it's not tied
 * to any persisted state), an ephemeral master key would permanently strand every already-
 * encrypted organization wallet the moment the process restarts.
 */
@Component
public class OrganizationWalletEncryptionService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int KEY_LENGTH_BYTES = 32;

    private final SecretKeySpec masterKey;
    private final SecureRandom secureRandom = new SecureRandom();

    OrganizationWalletEncryptionService(BlockchainProperties properties) {
        String base64Key = properties.getOrgWalletMasterKey();
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalStateException(
                    "rwashift.blockchain.org-wallet-master-key (ORG_WALLET_MASTER_KEY) is not set — required to "
                            + "encrypt/decrypt organization on-chain wallets at rest. Generate one with: "
                            + "openssl rand -base64 32");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("ORG_WALLET_MASTER_KEY is not valid base64", e);
        }
        if (keyBytes.length != KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "ORG_WALLET_MASTER_KEY must decode to exactly 32 bytes (AES-256); got " + keyBytes.length);
        }
        this.masterKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            ByteBuffer combined = ByteBuffer.allocate(iv.length + ciphertext.length);
            combined.put(iv).put(ciphertext);
            return Base64.getEncoder().encodeToString(combined.array());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt organization wallet key", e);
        }
    }

    public String decrypt(String stored) {
        try {
            byte[] combined = Base64.getDecoder().decode(stored);
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(combined, IV_LENGTH_BYTES, combined.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt organization wallet key", e);
        }
    }
}
