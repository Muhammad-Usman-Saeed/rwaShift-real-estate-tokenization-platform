import { apiDownload, apiRequest } from "@/lib/api/client";
import type { DocumentClassification, DocumentMetadataResponse } from "@/lib/api/types";

export const documentsApi = {
  upload: (
    params: { resourceType: string; resourceId: string; classification: DocumentClassification; file: File },
    token?: string,
  ) => {
    const formData = new FormData();
    formData.set("resourceType", params.resourceType);
    formData.set("resourceId", params.resourceId);
    formData.set("classification", params.classification);
    formData.set("file", params.file);
    return apiRequest<DocumentMetadataResponse>("/documents", { method: "POST", body: formData, isFormData: true, token });
  },
  download: (documentId: string, token?: string) => apiDownload(`/documents/${documentId}/download`, token),
  listForResource: (resourceType: string, resourceId: string, token?: string) =>
    apiRequest<DocumentMetadataResponse[]>(
      `/documents?resourceType=${encodeURIComponent(resourceType)}&resourceId=${encodeURIComponent(resourceId)}`,
      { token },
    ),
};
