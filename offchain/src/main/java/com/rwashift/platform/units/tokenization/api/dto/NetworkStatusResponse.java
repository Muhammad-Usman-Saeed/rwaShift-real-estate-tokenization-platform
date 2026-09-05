package com.rwashift.platform.units.tokenization.api.dto;

import com.rwashift.platform.units.tokenization.application.service.NetworkStatusApplicationService.ChainActivityItem;
import com.rwashift.platform.units.tokenization.application.service.NetworkStatusApplicationService.NetworkStatus;
import java.time.Instant;
import java.util.List;

public record NetworkStatusResponse(
        boolean reachable,
        String network,
        long chainId,
        Long latestBlockNumber,
        Double averageBlockTimeSeconds,
        List<ActivityItem> recentActivity) {

    public static NetworkStatusResponse from(NetworkStatus status) {
        return new NetworkStatusResponse(status.reachable(), status.network(), status.chainId(),
                status.latestBlockNumber(), status.averageBlockTimeSeconds(),
                status.recentActivity().stream().map(ActivityItem::from).toList());
    }

    public record ActivityItem(String eventName, String contractLabel, long blockNumber, String txHash, Instant blockTime) {

        static ActivityItem from(ChainActivityItem item) {
            return new ActivityItem(item.eventName(), item.contractLabel(), item.blockNumber(), item.txHash(), item.blockTime());
        }
    }
}
