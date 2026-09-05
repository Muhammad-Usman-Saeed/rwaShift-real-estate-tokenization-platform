package com.rwashift.platform.units.document.domain.repository;

import com.rwashift.platform.units.document.domain.model.DocumentMetadata;
import java.util.List;
import java.util.Optional;

public interface DocumentMetadataRepository {

    DocumentMetadata save(DocumentMetadata document);

    Optional<DocumentMetadata> findById(String id);

    List<DocumentMetadata> findByResourceTypeAndResourceId(String resourceType, String resourceId);
}
