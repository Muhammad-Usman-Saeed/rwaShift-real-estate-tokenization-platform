"use client";

import Link from "next/link";
import { PageHeader } from "@/components/ui/PageHeader";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/Table";
import { StatusPill } from "@/components/domain/StatusPill";
import { EmptyState } from "@/components/ui/EmptyState";
import { SkeletonTable } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { CircleDollar } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { investmentsApi } from "@/lib/api/investments";
import { queryKeys } from "@/lib/api/query-keys";
import { formatCurrency, formatDate } from "@/lib/utils/format";

const RESUMABLE_STATUSES = ["ELIGIBILITY_PENDING", "PAYMENT_PENDING"];

export default function InvestorInvestmentsPage() {
  const investments = useAuthedQuery(queryKeys.investments.mine(), (token) => investmentsApi.mine(token));

  return (
    <div>
      <PageHeader
        title="Investments"
        description={
          investments.data
            ? `${investments.data.length} investment${investments.data.length === 1 ? "" : "s"} you've made, in application-record detail.`
            : "Every investment you've made, in application-record detail."
        }
      />

      {investments.isLoading && <SkeletonTable rows={4} cols={5} />}
      {investments.isError && <ErrorBanner error={investments.error} />}

      {investments.data && investments.data.length === 0 && (
        <EmptyState icon={<CircleDollar className="h-8 w-8" />} title="No investments yet" />
      )}

      {investments.data && investments.data.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Date</TableHead>
              <TableHead>Amount</TableHead>
              <TableHead>Units</TableHead>
              <TableHead>Payment</TableHead>
              <TableHead>Status</TableHead>
              <TableHead className="text-right">Actions</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {investments.data.map((investment) => (
              <TableRow key={investment.id}>
                <TableCell>{formatDate(investment.createdAt)}</TableCell>
                <TableCell className="tabular-nums">{formatCurrency(investment.amount, investment.currency)}</TableCell>
                <TableCell className="tabular-nums">{investment.units.toLocaleString()}</TableCell>
                <TableCell>
                  <StatusPill status={investment.paymentStatus} />
                </TableCell>
                <TableCell>
                  <StatusPill status={investment.status} />
                </TableCell>
                <TableCell className="text-right">
                  {RESUMABLE_STATUSES.includes(investment.status) && (
                    <Link href={`/investor/investments/${investment.id}`} className="text-sm font-medium text-brand-700 hover:underline">
                      {investment.status === "PAYMENT_PENDING" ? "Resume Payment" : "View Status"}
                    </Link>
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
