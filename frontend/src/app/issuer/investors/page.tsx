"use client";

import { useMemo } from "react";
import Link from "next/link";
import { PageHeader } from "@/components/ui/PageHeader";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/Table";
import { EmptyState } from "@/components/ui/EmptyState";
import { SkeletonTable } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { Users } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { investmentsApi } from "@/lib/api/investments";
import { offeringsApi } from "@/lib/api/offerings";
import { queryKeys } from "@/lib/api/query-keys";
import { formatCurrency, truncateHex } from "@/lib/utils/format";

export default function IssuerInvestorsPage() {
  const investments = useAuthedQuery(queryKeys.investments.forOrganization(), (token) => investmentsApi.forOrganization(token));
  const offerings = useAuthedQuery(queryKeys.offerings.list(), (token) => offeringsApi.list(token));

  const byInvestor = useMemo(() => {
    if (!investments.data) return [];
    const map = new Map<
      string,
      { investorId: string; offeringIds: Set<string>; totalUnits: number; totalInvested: number; pendingCount: number; currency: string }
    >();
    for (const inv of investments.data) {
      const entry = map.get(inv.investorId) ??
        { investorId: inv.investorId, offeringIds: new Set<string>(), totalUnits: 0, totalInvested: 0, pendingCount: 0, currency: inv.currency };
      entry.offeringIds.add(inv.offeringId);
      // Only CONFIRMED payments count as money actually received — a pending or failed payment
      // isn't real capital yet, even though the Investment row already exists. See the dedicated
      // Payments page for the per-transaction breakdown this page intentionally doesn't show.
      if (inv.paymentStatus === "CONFIRMED") {
        entry.totalUnits += inv.units;
        entry.totalInvested += inv.amount;
      } else if (inv.paymentStatus === "PENDING") {
        entry.pendingCount += 1;
      }
      map.set(inv.investorId, entry);
    }
    return Array.from(map.values());
  }, [investments.data]);

  return (
    <div>
      <PageHeader
        title="Investors"
        description={
          investments.data
            ? `${byInvestor.length} investor${byInvestor.length === 1 ? "" : "s"} in your organization's offerings. Totals below count confirmed payments only — see Payments for pending or failed ones.`
            : "Investors who have invested in your organization's offerings. Totals below count confirmed payments only — see Payments for pending or failed ones."
        }
      />

      {investments.isLoading && <SkeletonTable rows={5} cols={4} />}
      {investments.isError && <ErrorBanner error={investments.error} />}

      {investments.data && byInvestor.length === 0 && (
        <EmptyState icon={<Users className="h-8 w-8" />} title="No investors yet" />
      )}

      {investments.data && byInvestor.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Investor</TableHead>
              <TableHead>Offerings</TableHead>
              <TableHead>Total Units</TableHead>
              <TableHead>Total Invested (Confirmed)</TableHead>
              <TableHead>Pending Payments</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {byInvestor.map((row) => (
              <TableRow key={row.investorId}>
                <TableCell className="font-mono text-xs">{truncateHex(row.investorId, 8, 4)}</TableCell>
                <TableCell>
                  {Array.from(row.offeringIds)
                    .map((id) => offerings.data?.find((o) => o.id === id)?.name ?? id)
                    .join(", ")}
                </TableCell>
                <TableCell className="tabular-nums">{row.totalUnits.toLocaleString()}</TableCell>
                <TableCell className="tabular-nums">{formatCurrency(row.totalInvested, row.currency)}</TableCell>
                <TableCell>
                  {row.pendingCount > 0 ? (
                    <Link href="/issuer/payments" className="text-sm font-medium text-warning-600 hover:underline">
                      {row.pendingCount} pending
                    </Link>
                  ) : (
                    <span className="text-sm text-ink-400">—</span>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
