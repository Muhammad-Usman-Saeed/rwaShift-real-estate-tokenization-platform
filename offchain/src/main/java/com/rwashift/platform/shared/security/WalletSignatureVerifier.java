package com.rwashift.platform.shared.security;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.Arrays;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

/**
 * ECDSA public-key recovery ({@code ecrecover}) for a "personal_sign"-style Ethereum signature —
 * the same scheme MetaMask/wagmi's {@code signMessage} produces, which internally prefixes the
 * message with {@code "\x19Ethereum Signed Message:\n" + length} before hashing. This never sees
 * a private key; it only proves which address's key must have produced a given signature over a
 * given message. See {@code WalletAuthenticationProvider} for how the result is used.
 */
public final class WalletSignatureVerifier {

    private static final int SIGNATURE_LENGTH_BYTES = 65;

    private WalletSignatureVerifier() {
    }

    public static String recoverAddress(String message, String signatureHex) {
        byte[] signatureBytes = Numeric.hexStringToByteArray(signatureHex);
        if (signatureBytes.length != SIGNATURE_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                    "Malformed signature: expected %d bytes, got %d".formatted(SIGNATURE_LENGTH_BYTES, signatureBytes.length));
        }
        byte[] r = Arrays.copyOfRange(signatureBytes, 0, 32);
        byte[] s = Arrays.copyOfRange(signatureBytes, 32, 64);
        byte v = signatureBytes[64];
        if (v < 27) {
            v += 27;
        }
        Sign.SignatureData signatureData = new Sign.SignatureData(v, r, s);
        try {
            BigInteger publicKey = Sign.signedPrefixedMessageToKey(message.getBytes(StandardCharsets.UTF_8), signatureData);
            return "0x" + Keys.getAddress(publicKey);
        } catch (SignatureException e) {
            throw new IllegalArgumentException("Could not recover a wallet address from this signature", e);
        }
    }
}
