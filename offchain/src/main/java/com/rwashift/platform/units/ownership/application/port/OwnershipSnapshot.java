package com.rwashift.platform.units.ownership.application.port;

import java.math.BigInteger;

public record OwnershipSnapshot(String investorId, String walletAddress, BigInteger units) {
}
