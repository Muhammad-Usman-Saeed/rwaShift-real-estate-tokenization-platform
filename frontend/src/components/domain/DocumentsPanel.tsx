"use client";

import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { FileUpload } from "@/components/ui/FileUpload";
import { Select } from "@/components/ui/Select";
import { Button } from "@/components/ui/Button";
import { EmptyState } from "@/components/ui/EmptyState";
import { SkeletonText } from "@/components/ui/Skeleton";
import { ErrorBanner } from "@/components/ui/ErrorBanner";
import { FileText } from "@/components/ui/icons";
import { useAuthedQuery } from "@/lib/hooks/useAuthedQuery";
import { useAuthedMutation } from "@/lib/hooks/useAuthedMutation";
import { useToast } from "@/components/ui/Toast";
import { documentsApi } from "@/lib/api/documents";
import { queryKeys } from "@/lib/api/query-keys";
import type { DocumentClassification } from "@/lib/api/types";

const CLASSIFICATION_OPTIONS = [
  { value: "PUBLIC", label: "Public" },
  { value: "ISSUER_CONFIDENTIAL", label: "Issuer Confidential" },
  { value: "INVESTOR_CONFIDENTIAL", label: "Investor Confidential" },
  { value: "COMPLIANCE_RESTRICTED", label: "Compliance Restricted" },
];

export function DocumentsPanel({ resourceType, resourceId }: { resourceType: string; resourceId: string }) {
  const queryClient = useQueryClient();
  const { push } = useToast();
  const [classification, setClassification] = useState<DocumentClassification>("ISSUER_CONFIDENTIAL");
  const [pendingFiles, setPendingFiles] = useState<File[]>([]);

  const documents = useAuthedQuery(queryKeys.documents.forResource(resourceType, resourceId), (token) =>
    documentsApi.listForResource(resourceType, resourceId, token),
  );

  const upload = useAuthedMutation((token, file: File) => documentsApi.upload({ resourceType, resourceId, classification, file }, token));

  function handleUpload() {
    const [file] = pendingFiles;
    if (!file) return;
    upload.mutate(file, {
      onSuccess: () => {
        queryClient.invalidateQueries({ queryKey: queryKeys.documents.forResource(resourceType, resourceId) });
        setPendingFiles([]);
        push({ title: "Document uploaded", description: file.name, variant: "success" });
      },
      onError: () => push({ title: "Upload failed", variant: "error" }),
    });
  }

  async function handleDownload(documentId: string, filename: string) {
    try {
      const { blob } = await documentsApi.download(documentId);
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
    <div className="flex flex-col gap-6">
      <div className="rounded-lg border border-surface-border bg-white p-4">
        <p className="mb-3 text-sm font-semibold text-ink-800">Upload document</p>
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end">
          <div className="sm:w-56">
            <Select value={classification} onValueChange={(v) => setClassification(v as DocumentClassification)} options={CLASSIFICATION_OPTIONS} />
          </div>
          <div className="flex-1">
            <FileUpload onFilesSelected={(files) => setPendingFiles(files.slice(0, 1))} selectedFiles={pendingFiles} onRemove={() => setPendingFiles([])} />
          </div>
          <Button onClick={handleUpload} disabled={!pendingFiles.length} isLoading={upload.isPending}>
            Upload
          </Button>
        </div>
        {upload.isError && <ErrorBanner error={upload.error} className="mt-3" />}
      </div>

      {documents.isLoading && <SkeletonText lines={3} />}
      {documents.isError && <ErrorBanner error={documents.error} />}
      {documents.data && documents.data.length === 0 && (
        <EmptyState icon={<FileText className="h-8 w-8" />} title="No documents yet" description="Uploaded files will appear here." />
      )}
      {documents.data && documents.data.length > 0 && (
        <ul className="divide-y divide-surface-border rounded-lg border border-surface-border bg-white">
          {documents.data.map((doc) => (
            <li key={doc.id} className="flex items-center justify-between gap-4 px-4 py-3">
              <div className="flex items-center gap-3 truncate">
                <FileText className="h-4 w-4 shrink-0 text-ink-400" />
                <div className="truncate">
                  <p className="truncate text-sm font-medium text-ink-800">{doc.filename}</p>
                  <p className="text-xs text-ink-500">{doc.classification}</p>
                </div>
              </div>
              <Button variant="ghost" size="sm" onClick={() => handleDownload(doc.id, doc.filename)}>
                Download
              </Button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
