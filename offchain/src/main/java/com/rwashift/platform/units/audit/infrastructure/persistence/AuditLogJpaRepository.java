package com.rwashift.platform.units.audit.infrastructure.persistence;

import com.rwashift.platform.units.audit.domain.model.AuditLogEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface AuditLogJpaRepository extends JpaRepository<AuditLogEntry, String> {

    List<AuditLogEntry> findByOrganizationId(String organizationId);

    List<AuditLogEntry> findByResourceTypeAndResourceId(String resourceType, String resourceId);

    List<AuditLogEntry> findAllByOrderByOccurredAtDesc();
}
