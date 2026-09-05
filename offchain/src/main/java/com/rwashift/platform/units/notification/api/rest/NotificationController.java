package com.rwashift.platform.units.notification.api.rest;

import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.notification.api.dto.NotificationItem;
import com.rwashift.platform.units.notification.application.service.NotificationApplicationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Every authenticated user gets whatever's relevant to their own roles/organization/investor id —
 * see {@link NotificationApplicationService}'s Javadoc for why this is computed on read rather
 * than a persisted, per-user notification table.
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationApplicationService notificationApplicationService;

    public NotificationController(NotificationApplicationService notificationApplicationService) {
        this.notificationApplicationService = notificationApplicationService;
    }

    @GetMapping
    public List<NotificationItem> list(TenantContext tenantContext) {
        return notificationApplicationService.listForCaller(tenantContext);
    }
}
