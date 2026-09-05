package com.rwashift.platform.units.document.application.service;

import com.rwashift.platform.shared.domain.NotFoundException;
import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.audit.application.port.AuditPort;
import com.rwashift.platform.units.document.application.port.DocumentStorage;
import com.rwashift.platform.units.document.domain.model.DocumentClassification;
import com.rwashift.platform.units.document.domain.model.DocumentMetadata;
import com.rwashift.platform.units.document.domain.repository.DocumentMetadataRepository;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentApplicationService {

    private final DocumentMetadataRepository documentMetadataRepository;
    private final DocumentStorage documentStorage;
    private final AuditPort auditPort;

    public DocumentApplicationService(DocumentMetadataRepository documentMetadataRepository, DocumentStorage documentStorage,
            AuditPort auditPort) {
        this.documentMetadataRepository = documentMetadataRepository;
        this.documentStorage = documentStorage;
        this.auditPort = auditPort;
    }

    @Transactional
    public DocumentMetadata upload(TenantContext caller, String resourceType, String resourceId, String filename,
            String contentType, byte[] content, DocumentClassification classification) {
        String storageKey = documentStorage.store(caller.organizationId(), filename, content);
        String sha256 = sha256Hex(content);
        DocumentMetadata document = new DocumentMetadata(caller.organizationId(), resourceType, resourceId, filename,
                contentType, storageKey, sha256, classification);
        document = documentMetadataRepository.save(document);
        auditPort.record(AuditPort.AuditEntry.of(caller.userId(), caller.organizationId(), "DOCUMENT_UPLOADED",
                "Document", document.getId(), null, resourceType + ":" + resourceId));
        return document;
    }

    /** Investors may download documents attached to any resource (e.g. an offering's memorandum); issuer-side callers only their own organization's. */
    @Transactional(readOnly = true)
    public byte[] download(TenantContext caller, String documentId) {
        DocumentMetadata document = documentMetadataRepository.findById(documentId)
                .orElseThrow(() -> new NotFoundException("Document", documentId));
        if (!caller.hasRole("INVESTOR")) {
            caller.requireSameOrganization(document.getOrganizationId());
        }
        return documentStorage.retrieve(document.getStorageKey());
    }

    @Transactional(readOnly = true)
    public List<DocumentMetadata> listForResource(String resourceType, String resourceId) {
        return documentMetadataRepository.findByResourceTypeAndResourceId(resourceType, resourceId);
    }

    private static String sha256Hex(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
