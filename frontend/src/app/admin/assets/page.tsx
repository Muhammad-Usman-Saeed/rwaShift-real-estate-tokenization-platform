"use client";

import { PageHeader } from "@/components/ui/PageHeader";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/Table";
import { StatusPill } from "@/components/domain/StatusPill";
import { EmptyState } from "@/components/ui/EmptyState";
import { SkeletonTable } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { Building } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { assetsApi } from "@/lib/api/assets";
import { organizationsApi } from "@/lib/api/organizations";
import { queryKeys } from "@/lib/api/query-keys";
import { formatCurrency } from "@/lib/utils/format";

export default function AdminAssetsPage() {
  const assets = useAuthedQuery(queryKeys.assets.list(), (t) => assetsApi.list(t));
  const organizations = useAuthedQuery(queryKeys.organizations.list(), (t) => organizationsApi.list(t));

  return (
    <div>
      <PageHeader
        title="Assets"
        description={
          assets.data
            ? `${assets.data.length} asset${assets.data.length === 1 ? "" : "s"} registered across every organization.`
            : "All assets registered across every organization."
        }
      />

      {assets.isLoading && <SkeletonTable rows={5} cols={5} />}
      {assets.isError && <ErrorBanner error={assets.error} />}

      {assets.data && assets.data.length === 0 && (
        <EmptyState icon={<Building className="h-8 w-8" />} title="No assets yet" description="Assets will appear here once an issuer registers one." />
      )}

      {assets.data && assets.data.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Organization</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Valuation</TableHead>
              <TableHead>Status</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {assets.data.map((asset) => (
              <TableRow key={asset.id}>
                <TableCell className="font-medium text-ink-900">{asset.name}</TableCell>
                <TableCell>{organizations.data?.find((o) => o.id === asset.organizationId)?.displayName ?? asset.organizationId}</TableCell>
                <TableCell className="capitalize">{asset.type.replaceAll("_", " ").toLowerCase()}</TableCell>
                <TableCell className="tabular-nums">{formatCurrency(asset.valuation, asset.currency)}</TableCell>
                <TableCell>
                  <StatusPill status={asset.status} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
