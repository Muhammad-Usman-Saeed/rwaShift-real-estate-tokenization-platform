package com.rwashift.platform.units.audit.domain.repository;

import com.rwashift.platform.units.audit.domain.model.AuditLogEntry;
import java.util.List;

public interface AuditLogRepository {

    AuditLogEntry save(AuditLogEntry entry);

    List<AuditLogEntry> findByOrganizationId(String organizationId);

    List<AuditLogEntry> findAllByOrderByOccurredAtDesc();

    List<AuditLogEntry> findByResourceTypeAndResourceId(String resourceType, String resourceId);
}
