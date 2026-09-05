package com.rwashift.platform.units.document.infrastructure.persistence;

import com.rwashift.platform.units.document.domain.model.DocumentMetadata;
import com.rwashift.platform.units.document.domain.repository.DocumentMetadataRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class DocumentMetadataRepositoryAdapter implements DocumentMetadataRepository {

    private final DocumentMetadataJpaRepository jpaRepository;

    DocumentMetadataRepositoryAdapter(DocumentMetadataJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public DocumentMetadata save(DocumentMetadata document) {
        return jpaRepository.save(document);
    }

    @Override
    public Optional<DocumentMetadata> findById(String id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<DocumentMetadata> findByResourceTypeAndResourceId(String resourceType, String resourceId) {
        return jpaRepository.findByResourceTypeAndResourceId(resourceType, resourceId);
    }
}
