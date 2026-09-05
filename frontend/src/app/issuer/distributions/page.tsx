"use client";

import Link from "next/link";
import { PageHeader } from "@/components/ui/PageHeader";
import { Button } from "@/components/ui/Button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/Table";
import { StatusPill } from "@/components/domain/StatusPill";
import { EmptyState } from "@/components/ui/EmptyState";
import { SkeletonTable } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { Coins } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { distributionsApi } from "@/lib/api/distributions";
import { offeringsApi } from "@/lib/api/offerings";
import { queryKeys } from "@/lib/api/query-keys";
import { formatCurrency, formatDate } from "@/lib/utils/format";

export default function IssuerDistributionsPage() {
  const distributions = useAuthedQuery(queryKeys.distributions.list(), (token) => distributionsApi.list(token));
  const offerings = useAuthedQuery(queryKeys.offerings.list(), (token) => offeringsApi.list(token));

  return (
    <div>
      <PageHeader
        title="Distributions"
        description={
          distributions.data
            ? `${distributions.data.length} distribution${distributions.data.length === 1 ? "" : "s"} paid out to investors.`
            : "Rental income and other distributions paid out to investors."
        }
        actions={
          <Link href="/issuer/distributions/new">
            <Button>Create Distribution</Button>
          </Link>
        }
      />

      {distributions.isLoading && <SkeletonTable rows={4} cols={4} />}
      {distributions.isError && <ErrorBanner error={distributions.error} />}

      {distributions.data && distributions.data.length === 0 && (
        <EmptyState
          icon={<Coins className="h-8 w-8" />}
          title="No distributions yet"
          description="Create a distribution to pay out rental income or other proceeds to investors."
          action={
            <Link href="/issuer/distributions/new">
              <Button size="sm">Create Distribution</Button>
            </Link>
          }
        />
      )}

      {distributions.data && distributions.data.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Offering</TableHead>
              <TableHead>Amount</TableHead>
              <TableHead>Record Date</TableHead>
              <TableHead>Status</TableHead>
              <TableHead />
            </TableRow>
          </TableHeader>
          <TableBody>
            {distributions.data.map((d) => (
              <TableRow key={d.id}>
                <TableCell className="font-medium text-ink-900">
                  {offerings.data?.find((o) => o.id === d.offeringId)?.name ?? d.offeringId}
                </TableCell>
                <TableCell className="tabular-nums">{formatCurrency(d.totalAmount, d.currency)}</TableCell>
                <TableCell>{formatDate(d.recordDate)}</TableCell>
                <TableCell>
                  <StatusPill status={d.status} />
                </TableCell>
                <TableCell className="text-right">
                  <Link href={`/issuer/distributions/${d.id}`} className="text-sm font-medium text-brand-700 hover:underline">
                    View
                  </Link>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
