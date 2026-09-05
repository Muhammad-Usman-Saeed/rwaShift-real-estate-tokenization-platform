"use client";

import Link from "next/link";
import { PageHeader } from "@/components/ui/PageHeader";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/Table";
import { StatusPill } from "@/components/domain/StatusPill";
import { EmptyState } from "@/components/ui/EmptyState";
import { SkeletonTable } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { Layers } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { offeringsApi } from "@/lib/api/offerings";
import { organizationsApi } from "@/lib/api/organizations";
import { queryKeys } from "@/lib/api/query-keys";
import { formatCurrency, formatPercent } from "@/lib/utils/format";

export default function AdminOfferingsPage() {
  const offerings = useAuthedQuery(queryKeys.offerings.list(), (t) => offeringsApi.list(t));
  const organizations = useAuthedQuery(queryKeys.organizations.list(), (t) => organizationsApi.list(t));

  return (
    <div>
      <PageHeader
        title="Offerings"
        description={
          offerings.data
            ? `${offerings.data.length} offering${offerings.data.length === 1 ? "" : "s"} across every organization.`
            : "All investment offerings across every organization."
        }
      />

      {offerings.isLoading && <SkeletonTable rows={5} cols={6} />}
      {offerings.isError && <ErrorBanner error={offerings.error} />}

      {offerings.data && offerings.data.length === 0 && (
        <EmptyState icon={<Layers className="h-8 w-8" />} title="No offerings yet" description="Offerings will appear here once an issuer creates one." />
      )}

      {offerings.data && offerings.data.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Offering</TableHead>
              <TableHead>Organization</TableHead>
              <TableHead>Raise</TableHead>
              <TableHead>Units Issued</TableHead>
              <TableHead>Status</TableHead>
              <TableHead />
            </TableRow>
          </TableHeader>
          <TableBody>
            {offerings.data.map((offering) => (
              <TableRow key={offering.id}>
                <TableCell className="font-medium text-ink-900">{offering.name}</TableCell>
                <TableCell>{organizations.data?.find((o) => o.id === offering.organizationId)?.displayName ?? offering.organizationId}</TableCell>
                <TableCell className="tabular-nums">{formatCurrency(offering.targetRaise, offering.currency)}</TableCell>
                <TableCell className="tabular-nums">
                  {offering.unitsIssued.toLocaleString()} / {offering.totalUnits.toLocaleString()} ({formatPercent((offering.unitsIssued / Math.max(offering.totalUnits, 1)) * 100)})
                </TableCell>
                <TableCell>
                  <StatusPill status={offering.status} />
                </TableCell>
                <TableCell className="text-right">
                  {offering.status === "UNDER_REVIEW" ? (
                    <Link href={`/admin/offering-approvals/${offering.id}`} className="text-sm font-medium text-brand-700 hover:underline">
                      Review
                    </Link>
                  ) : null}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
