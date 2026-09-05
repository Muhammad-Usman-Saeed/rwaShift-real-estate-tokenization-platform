import { StatusPill } from "@/components/domain/StatusPill";
import { ExternalLink } from "@/components/ui/icons";
import { CopyButton } from "@/components/ui/CopyButton";
import { explorerTxUrl } from "@/lib/wallet/explorer";
import { truncateHex } from "@/lib/utils/format";
import type { BlockchainTransactionStatus } from "@/lib/api/types";

export interface BlockchainTxDetailsProps {
  status: BlockchainTransactionStatus;
  network: string;
  txHash?: string | null;
  contractAddress?: string | null;
  blockNumber?: number | null;
  failureReason?: string | null;
}

/**
 * The one place transaction status is rendered — deliberately never lets `SUBMITTED`/`PENDING`
 * be displayed as `CONFIRMED` (see product spec section 24). Always shows the backend's actual
 * status, never an optimistic guess.
 */
export function BlockchainTxDetails({ status, network, txHash, contractAddress, blockNumber, failureReason }: BlockchainTxDetailsProps) {
  const explorerUrl = txHash ? explorerTxUrl(network, txHash) : null;

  return (
    <dl className="grid grid-cols-1 gap-x-6 gap-y-3 rounded-lg border border-surface-border bg-surface-subtle p-4 sm:grid-cols-2">
      <div>
        <dt className="text-xs font-medium uppercase tracking-wide text-ink-500">Status</dt>
        <dd className="mt-1">
          <StatusPill status={status} />
        </dd>
      </div>
      <div>
        <dt className="text-xs font-medium uppercase tracking-wide text-ink-500">Network</dt>
        <dd className="mt-1 text-sm capitalize text-ink-800">{network}</dd>
      </div>
      <div>
        <dt className="text-xs font-medium uppercase tracking-wide text-ink-500">Transaction Hash</dt>
        <dd className="mt-1 flex items-center gap-1.5 font-mono text-sm text-ink-800">
          {txHash ? (
            <>
              {explorerUrl ? (
                <a href={explorerUrl} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 text-brand-700 hover:underline">
                  {truncateHex(txHash)}
                  <ExternalLink className="h-3 w-3" />
                </a>
              ) : (
                truncateHex(txHash)
              )}
              <CopyButton value={txHash} label="Copy transaction hash" />
            </>
          ) : (
            <span className="text-ink-400">Not yet submitted</span>
          )}
        </dd>
      </div>
      <div>
        <dt className="text-xs font-medium uppercase tracking-wide text-ink-500">Block Number</dt>
        <dd className="mt-1 text-sm tabular-nums text-ink-800">{blockNumber ?? <span className="text-ink-400">Pending</span>}</dd>
      </div>
      {contractAddress && (
        <div className="sm:col-span-2">
          <dt className="text-xs font-medium uppercase tracking-wide text-ink-500">Contract Address</dt>
          <dd className="mt-1 flex items-center gap-1.5 font-mono text-sm text-ink-800">
            {truncateHex(contractAddress, 10, 8)}
            <CopyButton value={contractAddress} label="Copy contract address" />
          </dd>
        </div>
      )}
      {failureReason && (
        <div className="sm:col-span-2">
          <dt className="text-xs font-medium uppercase tracking-wide text-danger-600">Failure Reason</dt>
          <dd className="mt-1 text-sm text-danger-700">{failureReason}</dd>
        </div>
      )}
    </dl>
  );
}
