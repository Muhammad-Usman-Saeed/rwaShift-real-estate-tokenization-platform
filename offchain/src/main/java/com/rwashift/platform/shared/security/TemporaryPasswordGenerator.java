package com.rwashift.platform.shared.security;

import java.security.SecureRandom;

/**
 * Generates a one-time login password for an admin-provisioned account (see
 * {@code OrganizationUserController}). There's no email delivery in this platform yet, so the
 * plaintext is returned to the admin exactly once in the create-response for them to relay
 * out-of-band — never logged, never persisted (only its {@code PasswordEncoder} hash is stored).
 */
public final class TemporaryPasswordGenerator {

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
    private static final int LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TemporaryPasswordGenerator() {
    }

    public static String generate() {
        StringBuilder password = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            password.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return password.toString();
    }
}
