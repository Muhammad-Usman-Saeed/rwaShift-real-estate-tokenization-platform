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
import { assetsApi } from "@/lib/api/assets";
import { offeringsApi } from "@/lib/api/offerings";
import { documentsApi } from "@/lib/api/documents";
import { queryKeys } from "@/lib/api/query-keys";
import { useToast } from "@/components/ui/Toast";

export default function IssuerDocumentsPage() {
  const token = useAccessToken();
  const { push } = useToast();
  const assets = useAuthedQuery(queryKeys.assets.list(), (t) => assetsApi.list(t));
  const offerings = useAuthedQuery(queryKeys.offerings.list(), (t) => offeringsApi.list(t));

  const resources = [
    ...(assets.data ?? []).map((a) => ({ type: "Asset", id: a.id, label: a.name })),
    ...(offerings.data ?? []).map((o) => ({ type: "Offering", id: o.id, label: o.name })),
  ];

  const documentQueries = useQueries({
    queries: resources.map((resource) => ({
      queryKey: queryKeys.documents.forResource(resource.type, resource.id),
      queryFn: () => documentsApi.listForResource(resource.type, resource.id, token),
      enabled: !!token,
    })),
  });

  const isLoading = assets.isLoading || offerings.isLoading || documentQueries.some((q) => q.isLoading);
  const rows = resources.flatMap((resource, index) =>
    (documentQueries[index]?.data ?? []).map((doc) => ({ ...doc, resourceLabel: resource.label, resourceType: resource.type })),
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

  return (
    <div>
      <PageHeader
        title="Documents"
        description={
          !isLoading
            ? `${rows.length} document${rows.length === 1 ? "" : "s"} attached to your organization's assets and offerings.`
            : "All documents attached to your organization's assets and offerings."
        }
      />

      {isLoading && <SkeletonTable rows={5} cols={4} />}
      {assets.isError && <ErrorBanner error={assets.error} />}

      {!isLoading && rows.length === 0 && (
        <EmptyState icon={<FileText className="h-8 w-8" />} title="No documents yet" description="Upload documents from an asset or offering's Documents tab." />
      )}

      {!isLoading && rows.length > 0 && (
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>File</TableHead>
              <TableHead>Attached To</TableHead>
              <TableHead>Classification</TableHead>
              <TableHead />
            </TableRow>
          </TableHeader>
          <TableBody>
            {rows.map((doc) => (
              <TableRow key={doc.id}>
                <TableCell className="font-medium text-ink-900">{doc.filename}</TableCell>
                <TableCell>
                  {doc.resourceType}: {doc.resourceLabel}
                </TableCell>
                <TableCell>{doc.classification}</TableCell>
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
