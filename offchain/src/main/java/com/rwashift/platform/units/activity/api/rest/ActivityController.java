package com.rwashift.platform.units.activity.api.rest;

import com.rwashift.platform.shared.security.TenantContext;
import com.rwashift.platform.units.activity.api.dto.ActivityFeedItem;
import com.rwashift.platform.units.activity.api.dto.ActivityStatsResponse;
import com.rwashift.platform.units.activity.api.dto.VisitStatsResponse;
import com.rwashift.platform.units.activity.application.service.ActivityApplicationService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unlike {@code AuditController} (locked to admin/compliance roles, full detail including
 * previous/new state and actor ids), this is intentionally open to every authenticated user —
 * the news ticker and Activity page are meant to be visible platform-wide, and the feed/messages
 * here are already scrubbed to generic, non-sensitive summaries (see
 * {@link ActivityApplicationService}'s Javadoc).
 */
@RestController
@RequestMapping("/api/v1/activity")
public class ActivityController {

    private final ActivityApplicationService activityApplicationService;

    public ActivityController(ActivityApplicationService activityApplicationService) {
        this.activityApplicationService = activityApplicationService;
    }

    @GetMapping("/feed")
    public List<ActivityFeedItem> feed(@RequestParam(defaultValue = "50") int limit) {
        return activityApplicationService.getFeed(limit);
    }

    @GetMapping("/stats")
    public ActivityStatsResponse stats() {
        return activityApplicationService.getStats();
    }

    @GetMapping("/visit-stats")
    public VisitStatsResponse visitStats() {
        return activityApplicationService.getVisitStats();
    }

    @PostMapping("/visits")
    public ResponseEntity<Void> recordVisit(TenantContext tenantContext) {
        activityApplicationService.recordVisit(tenantContext);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
