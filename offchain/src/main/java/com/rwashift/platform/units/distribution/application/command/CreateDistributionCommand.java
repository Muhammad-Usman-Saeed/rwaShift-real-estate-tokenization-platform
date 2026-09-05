package com.rwashift.platform.units.distribution.application.command;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateDistributionCommand(String offeringId, BigDecimal totalAmount, String currency, Instant recordDate) {
}
