package com.rwashift.platform.units.document.infrastructure.persistence;

import com.rwashift.platform.units.document.domain.model.DocumentMetadata;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface DocumentMetadataJpaRepository extends JpaRepository<DocumentMetadata, String> {

    List<DocumentMetadata> findByResourceTypeAndResourceId(String resourceType, String resourceId);
}
