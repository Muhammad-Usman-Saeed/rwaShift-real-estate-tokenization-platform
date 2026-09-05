"use client";

import { useMemo, useState } from "react";
import { PageHeader } from "@/components/ui/PageHeader";
import { StatTile } from "@/components/ui/StatTile";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/Table";
import { StatusPill } from "@/components/domain/StatusPill";
import { EmptyState } from "@/components/ui/EmptyState";
import { SkeletonTable, SkeletonText } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { Receipt, CircleDollar, Clock, AlertTriangle } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { investmentsApi } from "@/lib/api/investments";
import { offeringsApi } from "@/lib/api/offerings";
import { queryKeys } from "@/lib/api/query-keys";
import { formatCurrency, formatDateTime, formatNumber, truncateHex } from "@/lib/utils/format";
import { cn } from "@/lib/utils/cn";
import type { PaymentStatus } from "@/lib/api/types";

const FILTERS: { value: PaymentStatus | "ALL"; label: string }[] = [
  { value: "ALL", label: "All" },
  { value: "PENDING", label: "Pending" },
  { value: "CONFIRMED", label: "Confirmed" },
  { value: "FAILED", label: "Failed" },
];

const PAYMENT_METHOD_LABEL: Record<string, string> = {
  BANK_TRANSFER: "Bank Transfer",
  CRYPTO_WALLET: "Crypto Wallet",
};

/**
 * "Did I actually get paid?" — the one question the old Investors page couldn't answer, since it
 * summed every investment's amount regardless of paymentStatus, silently counting pending/failed
 * money as if it were already in hand. This reads `paymentStatus` directly, per investment, so a
 * pending or failed payment is visible instead of hidden inside an aggregate total.
 */
export default function IssuerPaymentsPage() {
  const [filter, setFilter] = useState<PaymentStatus | "ALL">("ALL");
  const investments = useAuthedQuery(queryKeys.investments.forOrganization(), (token) => investmentsApi.forOrganization(token));
  const offerings = useAuthedQuery(queryKeys.offerings.list(), (token) => offeringsApi.list(token));

  const summary = useMemo(() => {
    const rows = investments.data ?? [];
    return {
      confirmedAmount: rows.filter((r) => r.paymentStatus === "CONFIRMED").reduce((sum, r) => sum + r.amount, 0),
      pendingCount: rows.filter((r) => r.paymentStatus === "PENDING").length,
      failedCount: rows.filter((r) => r.paymentStatus === "FAILED").length,
      currency: rows[0]?.currency ?? "USD",
    };
  }, [investments.data]);

  const filtered = useMemo(() => {
    const rows = investments.data ?? [];
    const sorted = [...rows].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    if (filter === "ALL") return sorted;
    return sorted.filter((r) => r.paymentStatus === filter);
  }, [investments.data, filter]);

  function offeringName(offeringId: string): string {
    return offerings.data?.find((o) => o.id === offeringId)?.name ?? offeringId;
  }

  return (
    <div>
      <PageHeader
        title="Payments"
        description={
          investments.data
            ? `${filtered.length} of ${investments.data.length} payment${investments.data.length === 1 ? "" : "s"} against your offerings, and whether it actually landed.`
            : "Every investment payment made against your offerings, and whether it actually landed."
        }
      />

      {investments.isLoading && <SkeletonText lines={2} />}
      {investments.isError && <ErrorBanner error={investments.error} className="mb-4" />}

      {investments.data && (
        <div className="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
          <StatTile
            label="Confirmed Payments"
            value={formatCurrency(summary.confirmedAmount, summary.currency)}
            icon={<CircleDollar className="h-5 w-5" />}
          />
          <StatTile label="Pending" value={formatNumber(summary.pendingCount)} icon={<Clock className="h-5 w-5" />} />
          <StatTile label="Failed" value={formatNumber(summary.failedCount)} icon={<AlertTriangle className="h-5 w-5" />} />
        </div>
      )}

      <div className="mb-4 flex gap-1">
        {FILTERS.map((f) => (
          <button
            key={f.value}
            type="button"
            onClick={() => setFilter(f.value)}
            className={cn(
              "rounded-md px-3 py-1.5 text-xs font-medium transition-colors",
              filter === f.value ? "bg-brand-700 text-white" : "text-ink-600 hover:bg-surface-muted",
            )}
          >
            {f.label}
          </button>
        ))}
      </div>

      {investments.isLoading && <SkeletonTable rows={5} cols={6} />}

      {investments.data && filtered.length === 0 && (
        <EmptyState icon={<Receipt className="h-8 w-8" />} title="No payments" description="Nothing matches this filter yet." />
      )}

      {filtered.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Offering</TableHead>
              <TableHead>Investor</TableHead>
              <TableHead>Amount</TableHead>
              <TableHead>Method</TableHead>
              <TableHead>Payment Status</TableHead>
              <TableHead>Reference</TableHead>
              <TableHead>Date</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filtered.map((inv) => (
              <TableRow key={inv.id}>
                <TableCell className="font-medium text-ink-900">{offeringName(inv.offeringId)}</TableCell>
                <TableCell className="font-mono text-xs">{truncateHex(inv.investorId, 8, 4)}</TableCell>
                <TableCell className="tabular-nums">{formatCurrency(inv.amount, inv.currency)}</TableCell>
                <TableCell>{PAYMENT_METHOD_LABEL[inv.paymentMethod] ?? inv.paymentMethod}</TableCell>
                <TableCell>
                  <StatusPill status={inv.paymentStatus} />
                </TableCell>
                <TableCell className="font-mono text-xs text-ink-500">
                  {inv.paymentTxHash ? truncateHex(inv.paymentTxHash, 8, 6) : "—"}
                </TableCell>
                <TableCell className="text-xs">{formatDateTime(inv.createdAt)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
