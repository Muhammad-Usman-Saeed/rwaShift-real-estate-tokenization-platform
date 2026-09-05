package com.rwashift.platform.units.audit.infrastructure.persistence;

import com.rwashift.platform.units.audit.domain.model.AuditLogEntry;
import com.rwashift.platform.units.audit.domain.repository.AuditLogRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
class AuditLogRepositoryAdapter implements AuditLogRepository {

    private final AuditLogJpaRepository jpaRepository;

    AuditLogRepositoryAdapter(AuditLogJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AuditLogEntry save(AuditLogEntry entry) {
        return jpaRepository.save(entry);
    }

    @Override
    public List<AuditLogEntry> findByOrganizationId(String organizationId) {
        return jpaRepository.findByOrganizationId(organizationId);
    }

    @Override
    public List<AuditLogEntry> findByResourceTypeAndResourceId(String resourceType, String resourceId) {
        return jpaRepository.findByResourceTypeAndResourceId(resourceType, resourceId);
    }

    @Override
    public List<AuditLogEntry> findAllByOrderByOccurredAtDesc() {
        return jpaRepository.findAllByOrderByOccurredAtDesc();
    }
}
