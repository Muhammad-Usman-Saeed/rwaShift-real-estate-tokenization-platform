"use client";

import Link from "next/link";
import { PageHeader } from "@/components/ui/PageHeader";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/Table";
import { StatusPill } from "@/components/domain/StatusPill";
import { EmptyState } from "@/components/ui/EmptyState";
import { SkeletonTable } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { ShieldCheck } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { offeringsApi } from "@/lib/api/offerings";
import { organizationsApi } from "@/lib/api/organizations";
import { queryKeys } from "@/lib/api/query-keys";
import { formatCurrency } from "@/lib/utils/format";

export default function OfferingApprovalsPage() {
  const offerings = useAuthedQuery(queryKeys.offerings.list(), (t) => offeringsApi.list(t));
  const organizations = useAuthedQuery(queryKeys.organizations.list(), (t) => organizationsApi.list(t));
  const pending = offerings.data?.filter((o) => o.status === "UNDER_REVIEW") ?? [];

  return (
    <div>
      <PageHeader
        title="Offering Approvals"
        description={
          offerings.data
            ? `${pending.length} offering${pending.length === 1 ? "" : "s"} awaiting compliance review before tokenization.`
            : "Offerings awaiting compliance review before tokenization."
        }
      />

      {offerings.isLoading && <SkeletonTable rows={4} cols={4} />}
      {offerings.isError && <ErrorBanner error={offerings.error} />}

      {offerings.data && pending.length === 0 && (
        <EmptyState icon={<ShieldCheck className="h-8 w-8" />} title="Nothing pending review" description="All submitted offerings have been reviewed." />
      )}

      {pending.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Offering</TableHead>
              <TableHead>Organization</TableHead>
              <TableHead>Raise</TableHead>
              <TableHead />
            </TableRow>
          </TableHeader>
          <TableBody>
            {pending.map((offering) => (
              <TableRow key={offering.id}>
                <TableCell className="font-medium text-ink-900">{offering.name}</TableCell>
                <TableCell>{organizations.data?.find((o) => o.id === offering.organizationId)?.displayName ?? offering.organizationId}</TableCell>
                <TableCell className="tabular-nums">{formatCurrency(offering.targetRaise, offering.currency)}</TableCell>
                <TableCell className="text-right">
                  <Link href={`/admin/offering-approvals/${offering.id}`} className="text-sm font-medium text-brand-700 hover:underline">
                    Review
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
