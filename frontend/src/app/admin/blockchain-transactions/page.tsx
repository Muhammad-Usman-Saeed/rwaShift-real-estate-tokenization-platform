"use client";

import { useQueryClient } from "@tanstack/react-query";
import { PageHeader } from "@/components/ui/PageHeader";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/Table";
import { StatusPill } from "@/components/domain/StatusPill";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { SkeletonTable } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { ExternalLink, Receipt } from "@/components/ui/icons";
import { CopyButton } from "@/components/ui/CopyButton";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useAuthedMutation } from "@/lib/hooks/useAuthedMutation";
import { useToast } from "@/components/ui/Toast";
import { tokenizationApi } from "@/lib/api/tokenization";
import { queryKeys } from "@/lib/api/query-keys";
import { formatDateTime, truncateHex } from "@/lib/utils/format";
import { explorerTxUrl } from "@/lib/wallet/explorer";

/** The only retryable failure today — every other on-chain write here is signed by the
 * platform's always-funded agent key, so only this org-wallet-signed step can fail for a
 * recoverable reason (the org wallet ran out of gas). See TokenizationApplicationService#retryTransaction. */
function isRetryable(tx: { businessReferenceType: string; method: string; status: string }): boolean {
  return tx.status === "FAILED" && tx.businessReferenceType === "OFFERING_DEPLOYMENT" && tx.method === "unpause";
}

export default function BlockchainTransactionsPage() {
  const queryClient = useQueryClient();
  const { push } = useToast();
  const transactions = useAuthedQuery(
    queryKeys.tokenization.transactions(),
    (t) => tokenizationApi.transactions(t),
    { refetchInterval: (query) => (query.state.data?.some((tx) => tx.status === "PENDING" || tx.status === "SUBMITTED") ? 5000 : false) },
  );

  const retry = useAuthedMutation((token, transactionId: string) => tokenizationApi.retryTransaction(transactionId, token), {
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.tokenization.transactions() });
      push({ title: "Retry submitted", description: "Resubmitted on-chain — status will update once confirmed.", variant: "success" });
    },
    onError: (error) => push({ title: "Retry failed", description: error instanceof Error ? error.message : undefined, variant: "error" }),
  });

  return (
    <div>
      <PageHeader
        title="Blockchain Transactions"
        description={
          transactions.data
            ? `${transactions.data.length} on-chain transaction${transactions.data.length === 1 ? "" : "s"} submitted, with real backend-reported status.`
            : "Every on-chain write the platform has submitted, with its real backend-reported status."
        }
      />

      {transactions.isLoading && <SkeletonTable rows={6} cols={7} />}
      {transactions.isError && <ErrorBanner error={transactions.error} />}

      {transactions.data && transactions.data.length === 0 && (
        <EmptyState icon={<Receipt className="h-8 w-8" />} title="No transactions yet" />
      )}

      {transactions.data && transactions.data.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Operation</TableHead>
              <TableHead>Network</TableHead>
              <TableHead>Transaction Hash</TableHead>
              <TableHead>Block</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Submitted</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {transactions.data.map((tx) => {
              const explorerUrl = tx.txHash ? explorerTxUrl(tx.network, tx.txHash) : null;
              return (
                <TableRow key={tx.id}>
                  <TableCell>
                    <p className="font-medium text-ink-900">{tx.businessReferenceType.replaceAll("_", " ")}</p>
                    <p className="text-xs text-ink-400">{tx.method}</p>
                  </TableCell>
                  <TableCell className="capitalize">{tx.network}</TableCell>
                  <TableCell className="font-mono text-xs">
                    {tx.txHash ? (
                      <span className="inline-flex items-center gap-1.5">
                        {explorerUrl ? (
                          <a href={explorerUrl} target="_blank" rel="noreferrer" className="inline-flex items-center gap-1 text-brand-700 hover:underline">
                            {truncateHex(tx.txHash)} <ExternalLink className="h-3 w-3" />
                          </a>
                        ) : (
                          truncateHex(tx.txHash)
                        )}
                        <CopyButton value={tx.txHash} label="Copy transaction hash" />
                      </span>
                    ) : (
                      <span className="text-ink-400">—</span>
                    )}
                  </TableCell>
                  <TableCell className="tabular-nums">{tx.blockNumber ?? "—"}</TableCell>
                  <TableCell>
                    <StatusPill status={tx.status} />
                    {tx.status === "FAILED" && tx.failureReason && (
                      <p className="mt-1 max-w-xs text-xs text-danger-600">{tx.failureReason}</p>
                    )}
                  </TableCell>
                  <TableCell>{tx.submittedAt ? formatDateTime(tx.submittedAt) : "—"}</TableCell>
                  <TableCell className="text-right">
                    {isRetryable(tx) && (
                      <Button
                        size="sm"
                        variant="secondary"
                        isLoading={retry.isPending && retry.variables === tx.id}
                        onClick={() => retry.mutate(tx.id)}
                      >
                        Retry
                      </Button>
                    )}
                  </TableCell>
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
