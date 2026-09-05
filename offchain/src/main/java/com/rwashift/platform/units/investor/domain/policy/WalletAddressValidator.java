package com.rwashift.platform.units.investor.domain.policy;

import com.rwashift.platform.shared.domain.DomainException;
import java.util.regex.Pattern;

/** Structural validation only (EIP-55 checksum verification is left to the wallet/client). */
public final class WalletAddressValidator {

    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^0x[a-fA-F0-9]{40}$");

    private WalletAddressValidator() {
    }

    public static void validate(String address) {
        if (address == null || !ADDRESS_PATTERN.matcher(address).matches()) {
            throw new DomainException("'%s' is not a valid wallet address".formatted(address));
        }
    }
}
