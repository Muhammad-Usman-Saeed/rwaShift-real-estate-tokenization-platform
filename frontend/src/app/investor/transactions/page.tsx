"use client";

import { PageHeader } from "@/components/ui/PageHeader";
import { StatTile } from "@/components/ui/StatTile";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/Table";
import { StatusPill } from "@/components/domain/StatusPill";
import { EmptyState } from "@/components/ui/EmptyState";
import { SkeletonText, SkeletonTable } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { Callout } from "@/components/ui/Callout";
import { Receipt, Coins, Building, Layers } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { investmentsApi } from "@/lib/api/investments";
import { queryKeys } from "@/lib/api/query-keys";
import { formatCurrency, formatDateTime, formatNumber } from "@/lib/utils/format";

export default function InvestorTransactionsPage() {
  const investments = useAuthedQuery(queryKeys.investments.mine(), (token) => investmentsApi.mine(token));

  const total = investments.data?.length ?? 0;
  const walletCount = investments.data?.filter((i) => i.paymentMethod === "CRYPTO_WALLET").length ?? 0;
  const bankCount = investments.data?.filter((i) => i.paymentMethod === "BANK_TRANSFER").length ?? 0;

  return (
    <div>
      <PageHeader
        title="Transactions"
        description={
          investments.data
            ? `${total} transaction${total === 1 ? "" : "s"} in your investment history.`
            : "Your investment transaction history."
        }
      />

      <Callout className="mb-4">
        For full on-chain transaction detail (hash, block, contract), see the contract addresses in your Portfolio&apos;s
        On-Chain Holdings section and look them up on the network explorer.
      </Callout>

      {investments.isLoading && <SkeletonText lines={2} />}
      {investments.isError && <ErrorBanner error={investments.error} />}

      {investments.data && (
        <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
          <StatTile label="Total Transactions" value={formatNumber(total)} icon={<Layers className="h-5 w-5" />} />
          <StatTile label="Wallet Payment" value={formatNumber(walletCount)} icon={<Coins className="h-5 w-5" />} />
          <StatTile label="Bank Transfer" value={formatNumber(bankCount)} icon={<Building className="h-5 w-5" />} />
        </div>
      )}

      {investments.isLoading && <SkeletonTable rows={4} cols={5} />}

      {investments.data && investments.data.length === 0 && (
        <EmptyState icon={<Receipt className="h-8 w-8" />} title="No transactions yet" />
      )}

      {investments.data && investments.data.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Date</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Amount</TableHead>
              <TableHead>Status</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {investments.data.map((investment) => (
              <TableRow key={investment.id}>
                <TableCell>{formatDateTime(investment.updatedAt)}</TableCell>
                <TableCell>Investment</TableCell>
                <TableCell className="tabular-nums">{formatCurrency(investment.amount, investment.currency)}</TableCell>
                <TableCell>
                  <StatusPill status={investment.status} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
