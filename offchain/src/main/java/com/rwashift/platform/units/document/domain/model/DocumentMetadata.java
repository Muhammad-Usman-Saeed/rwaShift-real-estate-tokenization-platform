package com.rwashift.platform.units.document.domain.model;

import com.rwashift.platform.shared.domain.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/**
 * Document metadata only — the binary content lives behind {@link com.rwashift.platform.units.document.application.port.DocumentStorage}
 * (filesystem locally, S3-compatible in a real deployment), keyed by {@link #storageKey}. Never
 * stored in MySQL, and never on-chain (see README security notes).
 */
@Entity
@Table(name = "document_metadata")
public class DocumentMetadata extends TenantScopedEntity {

    @Column(name = "resource_type", nullable = false, length = 64)
    private String resourceType;

    @Column(name = "resource_id", nullable = false, length = 26)
    private String resourceId;

    @Column(name = "filename", nullable = false, length = 300)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification", nullable = false, length = 32)
    private DocumentClassification classification;

    protected DocumentMetadata() {
    }

    public DocumentMetadata(String organizationId, String resourceType, String resourceId, String filename,
            String contentType, String storageKey, String sha256, DocumentClassification classification) {
        super(organizationId);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.filename = filename;
        this.contentType = contentType;
        this.storageKey = storageKey;
        this.sha256 = sha256;
        this.classification = classification;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public String getSha256() {
        return sha256;
    }

    public DocumentClassification getClassification() {
        return classification;
    }
}
