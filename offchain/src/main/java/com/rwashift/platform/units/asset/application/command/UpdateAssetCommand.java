package com.rwashift.platform.units.asset.application.command;

import java.math.BigDecimal;

public record UpdateAssetCommand(
        String name,
        String description,
        String location,
        BigDecimal valuation,
        String currency
) {
}
