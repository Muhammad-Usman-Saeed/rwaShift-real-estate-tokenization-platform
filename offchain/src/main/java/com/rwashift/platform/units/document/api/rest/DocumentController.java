package com.rwashift.platform.units.document.api.rest;

import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.document.api.dto.DocumentMetadataResponse;
import com.rwashift.platform.units.document.application.service.DocumentApplicationService;
import com.rwashift.platform.units.document.domain.model.DocumentClassification;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentApplicationService documentApplicationService;

    public DocumentController(DocumentApplicationService documentApplicationService) {
        this.documentApplicationService = documentApplicationService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentMetadataResponse> upload(TenantContext tenantContext,
            @RequestParam String resourceType, @RequestParam String resourceId,
            @RequestParam DocumentClassification classification, @RequestParam MultipartFile file) throws IOException {
        var document = documentApplicationService.upload(tenantContext, resourceType, resourceId, file.getOriginalFilename(),
                file.getContentType(), file.getBytes(), classification);
        return ResponseEntity.created(URI.create("/api/v1/documents/" + document.getId())).body(DocumentMetadataResponse.from(document));
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<byte[]> download(TenantContext tenantContext, @PathVariable String documentId) {
        byte[] content = documentApplicationService.download(tenantContext, documentId);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).body(content);
    }

    @GetMapping
    public List<DocumentMetadataResponse> listForResource(@RequestParam String resourceType, @RequestParam String resourceId) {
        return documentApplicationService.listForResource(resourceType, resourceId).stream()
                .map(DocumentMetadataResponse::from)
                .toList();
    }
}
