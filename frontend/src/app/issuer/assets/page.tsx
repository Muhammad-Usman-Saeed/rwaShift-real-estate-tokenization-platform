"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { PageHeader } from "@/components/ui/PageHeader";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { Select } from "@/components/ui/Select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/Table";
import { StatusPill } from "@/components/domain/StatusPill";
import { EmptyState } from "@/components/ui/EmptyState";
import { SkeletonTable } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { Building, Search } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { assetsApi } from "@/lib/api/assets";
import { queryKeys } from "@/lib/api/query-keys";
import { formatCurrency } from "@/lib/utils/format";

const TYPE_OPTIONS = [
  { value: "ALL", label: "All types" },
  { value: "COMMERCIAL", label: "Commercial" },
  { value: "RESIDENTIAL", label: "Residential" },
  { value: "INDUSTRIAL", label: "Industrial" },
  { value: "MIXED_USE", label: "Mixed Use" },
  { value: "LAND", label: "Land" },
];

export default function IssuerAssetsPage() {
  const [search, setSearch] = useState("");
  const [type, setType] = useState("ALL");

  const assets = useAuthedQuery(queryKeys.assets.list(), (token) => assetsApi.list(token));

  const filtered = useMemo(() => {
    if (!assets.data) return [];
    return assets.data.filter((asset) => {
      const matchesSearch =
        !search ||
        asset.name.toLowerCase().includes(search.toLowerCase()) ||
        asset.location.toLowerCase().includes(search.toLowerCase());
      const matchesType = type === "ALL" || asset.type === type;
      return matchesSearch && matchesType;
    });
  }, [assets.data, search, type]);

  return (
    <div>
      <PageHeader
        title="Assets"
        description={
          assets.data
            ? `${filtered.length} of ${assets.data.length} asset${assets.data.length === 1 ? "" : "s"} held by your organization.`
            : "Physical real estate assets held by your organization."
        }
        actions={
          <Link href="/issuer/assets/new">
            <Button>Create Asset</Button>
          </Link>
        }
      />

      <div className="mb-4 flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-400" />
          <Input
            placeholder="Search by name or location…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9"
            aria-label="Search assets"
          />
        </div>
        <div className="sm:w-56">
          <Select value={type} onValueChange={setType} options={TYPE_OPTIONS} />
        </div>
      </div>

      {assets.isLoading && <SkeletonTable rows={5} cols={5} />}
      {assets.isError && <ErrorBanner error={assets.error} />}

      {assets.data && filtered.length === 0 && (
        <EmptyState
          icon={<Building className="h-8 w-8" />}
          title="No assets found"
          description="Create your first asset to begin structuring a tokenized offering."
          action={
            <Link href="/issuer/assets/new">
              <Button size="sm">Create Asset</Button>
            </Link>
          }
        />
      )}

      {assets.data && filtered.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Name</TableHead>
              <TableHead>Type</TableHead>
              <TableHead>Location</TableHead>
              <TableHead>Valuation</TableHead>
              <TableHead>Status</TableHead>
              <TableHead />
            </TableRow>
          </TableHeader>
          <TableBody>
            {filtered.map((asset) => (
              <TableRow key={asset.id}>
                <TableCell className="font-medium text-ink-900">{asset.name}</TableCell>
                <TableCell className="capitalize">{asset.type.replaceAll("_", " ").toLowerCase()}</TableCell>
                <TableCell>{asset.location}</TableCell>
                <TableCell className="tabular-nums">{formatCurrency(asset.valuation, asset.currency)}</TableCell>
                <TableCell>
                  <StatusPill status={asset.status} />
                </TableCell>
                <TableCell className="text-right">
                  <Link href={`/issuer/assets/${asset.id}`} className="text-sm font-medium text-brand-700 hover:underline">
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
