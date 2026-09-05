package com.rwashift.platform.units.document.api.dto;

import com.rwashift.platform.units.document.domain.model.DocumentMetadata;

public record DocumentMetadataResponse(
        String id,
        String resourceType,
        String resourceId,
        String filename,
        String contentType,
        String sha256,
        String classification
) {
    public static DocumentMetadataResponse from(DocumentMetadata d) {
        return new DocumentMetadataResponse(d.getId(), d.getResourceType(), d.getResourceId(), d.getFilename(),
                d.getContentType(), d.getSha256(), d.getClassification().name());
    }
}
