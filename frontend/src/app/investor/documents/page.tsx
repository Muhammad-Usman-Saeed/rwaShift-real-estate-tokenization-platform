"use client";

import { useQueries } from "@tanstack/react-query";
import { PageHeader } from "@/components/ui/PageHeader";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/Table";
import { EmptyState } from "@/components/ui/EmptyState";
import { SkeletonTable } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { Button } from "@/components/ui/Button";
import { FileText } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useAccessToken } from "@/lib/auth/session";
import { investmentsApi } from "@/lib/api/investments";
import { offeringsApi } from "@/lib/api/offerings";
import { documentsApi } from "@/lib/api/documents";
import { queryKeys } from "@/lib/api/query-keys";
import { useToast } from "@/components/ui/Toast";

export default function InvestorDocumentsPage() {
  const token = useAccessToken();
  const { push } = useToast();
  const investments = useAuthedQuery(queryKeys.investments.mine(), (t) => investmentsApi.mine(t));
  const offeringIds = Array.from(new Set((investments.data ?? []).map((i) => i.offeringId)));

  const offeringQueries = useQueries({
    queries: offeringIds.map((id) => ({ queryKey: queryKeys.offerings.detail(id), queryFn: () => offeringsApi.get(id, token), enabled: !!token })),
  });
  const documentQueries = useQueries({
    queries: offeringIds.map((id) => ({
      queryKey: queryKeys.documents.forResource("Offering", id),
      queryFn: () => documentsApi.listForResource("Offering", id, token),
      enabled: !!token,
    })),
  });

  const rows = offeringIds.flatMap((id, i) =>
    (documentQueries[i]?.data ?? []).map((doc) => ({ ...doc, offeringName: offeringQueries[i]?.data?.name })),
  );

  async function handleDownload(documentId: string, filename: string) {
    try {
      const { blob } = await documentsApi.download(documentId, token);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = filename;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      push({ title: "Download failed", variant: "error" });
    }
  }

  const isLoading = investments.isLoading || documentQueries.some((q) => q.isLoading);

  return (
    <div>
      <PageHeader
        title="Documents"
        description={
          !isLoading
            ? `${rows.length} document${rows.length === 1 ? "" : "s"} for investments you hold.`
            : "Offering documents for investments you hold."
        }
      />

      {isLoading && <SkeletonTable rows={4} cols={3} />}
      {investments.isError && <ErrorBanner error={investments.error} />}

      {!isLoading && rows.length === 0 && <EmptyState icon={<FileText className="h-8 w-8" />} title="No documents yet" />}

      {!isLoading && rows.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>File</TableHead>
              <TableHead>Offering</TableHead>
              <TableHead />
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((doc) => (
              <TableRow key={doc.id}>
                <TableCell className="font-medium text-ink-900">{doc.filename}</TableCell>
                <TableCell>{doc.offeringName}</TableCell>
                <TableCell className="text-right">
                  <Button variant="ghost" size="sm" onClick={() => handleDownload(doc.id, doc.filename)}>
                    Download
                  </Button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </div>
  );
}
