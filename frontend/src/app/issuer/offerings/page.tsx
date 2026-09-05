"use client";

import Link from "next/link";
import { PageHeader } from "@/components/ui/PageHeader";
import { Button } from "@/components/ui/Button";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/Table";
import { StatusPill } from "@/components/domain/StatusPill";
import { EmptyState } from "@/components/ui/EmptyState";
import { SkeletonTable } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { Layers } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { offeringsApi } from "@/lib/api/offerings";
import { queryKeys } from "@/lib/api/query-keys";
import { formatCurrency, formatPercent } from "@/lib/utils/format";

export default function IssuerOfferingsPage() {
  const offerings = useAuthedQuery(queryKeys.offerings.list(), (token) => offeringsApi.list(token));

  return (
    <div>
      <PageHeader
        title="Offerings"
        description={
          offerings.data
            ? `${offerings.data.length} offering${offerings.data.length === 1 ? "" : "s"} issued against your organization's assets.`
            : "Investment offerings issued against your organization's assets."
        }
        actions={
          <Link href="/issuer/offerings/new">
            <Button>Create Offering</Button>
          </Link>
        }
      />

      {offerings.isLoading && <SkeletonTable rows={5} cols={5} />}
      {offerings.isError && <ErrorBanner error={offerings.error} />}

      {offerings.data && offerings.data.length === 0 && (
        <EmptyState icon={<Layers className="h-8 w-8" />} title="No offerings yet" description="Create your first offering to begin raising capital." />
      )}

      {offerings.data && offerings.data.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Offering</TableHead>
              <TableHead>Raise</TableHead>
              <TableHead>Units Issued</TableHead>
              <TableHead>Funded</TableHead>
              <TableHead>Status</TableHead>
              <TableHead />
            </TableRow>
          </TableHeader>
          <TableBody>
            {offerings.data.map((offering) => {
              const pctFunded = offering.totalUnits > 0 ? (offering.unitsIssued / offering.totalUnits) * 100 : 0;
              return (
                <TableRow key={offering.id}>
                  <TableCell className="font-medium text-ink-900">{offering.name}</TableCell>
                  <TableCell>{formatCurrency(offering.targetRaise, offering.currency)}</TableCell>
                  <TableCell className="tabular-nums">
                    {offering.unitsIssued.toLocaleString()} / {offering.totalUnits.toLocaleString()}
                  </TableCell>
                  <TableCell className="tabular-nums">{formatPercent(pctFunded)}</TableCell>
                  <TableCell>
                    <StatusPill status={offering.status} />
                  </TableCell>
                  <TableCell className="text-right">
                    <Link href={`/issuer/offerings/${offering.id}`} className="text-sm font-medium text-brand-700 hover:underline">
                      View
                    </Link>
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
