package com.rwashift.platform.units.asset.application.command;

import com.rwashift.platform.units.asset.domain.model.AssetType;
import java.math.BigDecimal;

public record CreateAssetCommand(
        String name,
        AssetType type,
        String description,
        String location,
        BigDecimal valuation,
        String currency
) {
}
