package com.rwashift.platform.units.audit.application.service;

import com.rwashift.platform.shared.web.CorrelationIdFilter;
import com.rwashift.platform.units.audit.application.port.AuditPort;
import com.rwashift.platform.units.audit.domain.model.AuditLogEntry;
import com.rwashift.platform.units.audit.domain.repository.AuditLogRepository;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AuditApplicationService implements AuditPort {

    private final AuditLogRepository auditLogRepository;

    public AuditApplicationService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    @Transactional
    public void record(AuditEntry entry) {
        String correlationId = entry.correlationId() != null ? entry.correlationId() : MDC.get(CorrelationIdFilter.MDC_KEY);
        String ipAddress = currentRequestIp();
        auditLogRepository.save(new AuditLogEntry(entry.actorUserId(), entry.organizationId(), entry.action(),
                entry.resourceType(), entry.resourceId(), entry.previousState(), entry.newState(), correlationId,
                ipAddress, entry.blockchainTxHash()));
    }

    @Transactional(readOnly = true)
    public List<AuditLogEntry> getForOrganization(String organizationId) {
        return auditLogRepository.findByOrganizationId(organizationId);
    }

    /** Platform-wide audit trail, including system-actor entries with no organization (KYC, tokenization). */
    @Transactional(readOnly = true)
    public List<AuditLogEntry> getAll() {
        return auditLogRepository.findAllByOrderByOccurredAtDesc();
    }

    @Transactional(readOnly = true)
    public List<AuditLogEntry> getForResource(String resourceType, String resourceId) {
        return auditLogRepository.findByResourceTypeAndResourceId(resourceType, resourceId);
    }

    private static String currentRequestIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest().getRemoteAddr();
        }
        return null;
    }
}
