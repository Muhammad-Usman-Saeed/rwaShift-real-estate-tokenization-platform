"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { networkApi } from "@/lib/api/network";
import { CHAINS } from "@/lib/utils/constants";
import { NEXT_PUBLIC_CHAIN_ID, NEXT_PUBLIC_CHAIN_LABEL } from "@/lib/utils/env";
import { formatNumber, formatRelativeTime, truncateHex } from "@/lib/utils/format";

type ChainKey = keyof typeof CHAINS;

const CHAIN_KEYS = Object.keys(CHAINS) as ChainKey[];
const LIVE_CHAIN_KEY = CHAIN_KEYS.find((key) => CHAINS[key].id === NEXT_PUBLIC_CHAIN_ID) ?? "sepolia";

/**
 * Pre-login network insight + chain preview, shown above the sign-in button. There is
 * deliberately no "switch chain" control that pretends to change what this deployment talks to —
 * exactly one network is ever live (the on-chain contracts only exist on whichever chain
 * `docker-compose.yml`'s `contracts-deploy` actually deployed to). The toggle below only *previews*
 * the two chains this build is configured for; selecting the non-live one shows an honest
 * "not connected" state rather than faking data for it.
 *
 * Live figures come straight from `/api/v1/public/network-status` (unauthenticated, real RPC
 * reads — see `NetworkStatusApplicationService` on the backend), never invented client-side.
 */
export function NetworkStatusPanel() {
  const [previewChain, setPreviewChain] = useState<ChainKey>(LIVE_CHAIN_KEY);
  const isLive = previewChain === LIVE_CHAIN_KEY;

  const { data, isLoading, isError } = useQuery({
    queryKey: ["network-status"],
    queryFn: networkApi.status,
    refetchInterval: 20_000,
    retry: 1,
  });

  return (
    <div className="mb-6 rounded-lg border border-ink-800 bg-ink-900 p-5">
      <div className="flex items-center justify-between gap-3">
        <p className="text-xs font-semibold uppercase tracking-[0.15em] text-ink-400">Connected Network</p>
        <div role="tablist" aria-label="Preview a configured network" className="flex rounded-md border border-ink-700 bg-ink-950 p-0.5">
          {CHAIN_KEYS.map((key) => (
            <button
              key={key}
              type="button"
              role="tab"
              aria-selected={previewChain === key}
              onClick={() => setPreviewChain(key)}
              className={`rounded px-2.5 py-1 text-xs font-medium transition-colors ${
                previewChain === key ? "bg-ink-800 text-white" : "text-ink-400 hover:text-ink-200"
              }`}
            >
              {CHAINS[key].name}
            </button>
          ))}
        </div>
      </div>

      {!isLive ? (
        <div className="mt-4 rounded-md border border-dashed border-ink-700 px-4 py-6 text-center">
          <p className="text-sm text-ink-300">
            This deployment isn&apos;t connected to {CHAINS[previewChain].name} (chain ID {CHAINS[previewChain].id}).
          </p>
          <p className="mt-1 text-xs text-ink-500">Switch back to {NEXT_PUBLIC_CHAIN_LABEL} to see live network activity.</p>
        </div>
      ) : isLoading ? (
        <div className="mt-4 space-y-2" aria-label="Loading network status">
          <div className="h-4 w-2/3 animate-pulse rounded bg-ink-800" />
          <div className="h-4 w-1/2 animate-pulse rounded bg-ink-800" />
        </div>
      ) : isError || !data?.reachable ? (
        <div className="mt-4 flex items-center gap-2 rounded-md border border-danger-600/40 bg-danger-700/10 px-4 py-3">
          <span className="h-2 w-2 shrink-0 rounded-full bg-danger-500" aria-hidden="true" />
          <p className="text-sm text-danger-50">
            Unable to reach {NEXT_PUBLIC_CHAIN_LABEL} (chain ID {NEXT_PUBLIC_CHAIN_ID}) right now.
          </p>
        </div>
      ) : (
        <>
          <div className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-4">
            <Stat label="Network" value={data.network || NEXT_PUBLIC_CHAIN_LABEL} />
            <Stat label="Chain ID" value={String(data.chainId)} />
            <Stat label="Latest block" value={data.latestBlockNumber !== null ? formatNumber(data.latestBlockNumber) : "—"} />
            <Stat
              label="Avg. block time"
              value={data.averageBlockTimeSeconds !== null ? `${data.averageBlockTimeSeconds.toFixed(1)}s` : "—"}
            />
          </div>

          <div className="mt-5">
            <p className="text-xs font-semibold uppercase tracking-[0.15em] text-ink-400">Recent Chain Activity</p>
            {data.recentActivity.length === 0 ? (
              <p className="mt-2 text-sm text-ink-500">No on-chain activity from platform contracts yet.</p>
            ) : (
              <ul className="mt-2 space-y-2">
                {data.recentActivity.map((item) => (
                  <li key={`${item.txHash}-${item.blockNumber}`} className="flex items-center justify-between gap-3 text-sm">
                    <div className="min-w-0">
                      <p className="truncate text-ink-200">{item.eventName}</p>
                      <p className="truncate text-xs text-ink-500">
                        {item.contractLabel} · block {formatNumber(item.blockNumber)} · {truncateHex(item.txHash)}
                      </p>
                    </div>
                    {item.blockTime && <span className="shrink-0 text-xs text-ink-500">{formatRelativeTime(item.blockTime)}</span>}
                  </li>
                ))}
              </ul>
            )}
          </div>
        </>
      )}
    </div>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs text-ink-500">{label}</p>
      <p className="mt-0.5 text-sm font-semibold text-white">{value}</p>
    </div>
  );
}
