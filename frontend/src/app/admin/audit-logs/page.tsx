"use client";

import { useMemo, useState } from "react";
import { PageHeader } from "@/components/ui/PageHeader";
import { Input } from "@/components/ui/Input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/Table";
import { EmptyState } from "@/components/ui/EmptyState";
import { SkeletonTable } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { FileText, Search } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { auditApi } from "@/lib/api/audit";
import { queryKeys } from "@/lib/api/query-keys";
import { formatDateTime, truncateHex } from "@/lib/utils/format";

export default function AuditLogsPage() {
  const [search, setSearch] = useState("");
  const audit = useAuthedQuery(queryKeys.audit.list(), (t) => auditApi.list(t));

  const filtered = useMemo(() => {
    if (!audit.data) return [];
    const sorted = [...audit.data].sort((a, b) => new Date(b.occurredAt).getTime() - new Date(a.occurredAt).getTime());
    if (!search) return sorted;
    return sorted.filter(
      (e) =>
        e.action.toLowerCase().includes(search.toLowerCase()) ||
        e.resourceType.toLowerCase().includes(search.toLowerCase()) ||
        e.resourceId.toLowerCase().includes(search.toLowerCase()),
    );
  }, [audit.data, search]);

  return (
    <div>
      <PageHeader
        title="Audit Logs"
        description={
          audit.data
            ? `${filtered.length} of ${audit.data.length} entr${audit.data.length === 1 ? "y" : "ies"} — append-only record of every significant platform action.`
            : "Append-only record of every significant platform action."
        }
      />

      <div className="relative mb-4 max-w-sm">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-400" />
        <Input placeholder="Search by action or resource…" value={search} onChange={(e) => setSearch(e.target.value)} className="pl-9" aria-label="Search audit log" />
      </div>

      {audit.isLoading && <SkeletonTable rows={8} cols={5} />}
      {audit.isError && <ErrorBanner error={audit.error} />}

      {audit.data && filtered.length === 0 && <EmptyState icon={<FileText className="h-8 w-8" />} title="No matching audit entries" />}

      {filtered.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Action</TableHead>
              <TableHead>Resource</TableHead>
              <TableHead>State Change</TableHead>
              <TableHead>Actor</TableHead>
              <TableHead>Correlation</TableHead>
              <TableHead>Occurred</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filtered.map((entry) => (
              <TableRow key={entry.id}>
                <TableCell className="font-medium text-ink-900">{entry.action.replaceAll("_", " ")}</TableCell>
                <TableCell>
                  {entry.resourceType} · <span className="font-mono text-xs">{truncateHex(entry.resourceId, 8, 4)}</span>
                </TableCell>
                <TableCell className="text-xs text-ink-500">
                  {entry.previousState ?? "—"} → {entry.newState ?? "—"}
                </TableCell>
                <TableCell className="font-mono text-xs">{entry.actorUserId ? truncateHex(entry.actorUserId, 8, 4) : "system"}</TableCell>
                <TableCell className="font-mono text-xs text-ink-400">{entry.correlationId ? truncateHex(entry.correlationId, 6, 4) : "—"}</TableCell>
                <TableCell className="text-xs">{formatDateTime(entry.occurredAt)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
