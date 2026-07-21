package com.controlleads.analytics;

import com.controlleads.common.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only analytics. Every figure derives from the immutable
 * lead_status_events (CLAUDE.md rule 2) and is scoped to the caller's own
 * leads for MARKETING_TEAM, global for ADMINISTRATOR.
 */
@RestController
@Tag(name = "analytics")
public class AnalyticsController {

    public record Summary(long totalLeads, long newLeads, double conversionRate,
                          Double avgDaysToConvert, long slaBreaches) {}
    public record FunnelStage(String status, long count) {}
    public record DropOffRow(String stage, UUID reasonId, String reasonName, long count) {}
    public record TimePoint(String period, long created, long converted) {}
    public record BreakdownRow(UUID id, String name, long total, long students) {}
    public record CountryRow(String countryCode, long total, long students) {}
    public record LeaderboardRow(UUID userId, String name, long totalLeads, long students) {}

    private final AnalyticsService analytics;

    public AnalyticsController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    @Operation(summary = "Headline KPIs")
    @GetMapping("/api/analytics/summary")
    public Summary summary(@AuthenticationPrincipal Jwt jwt,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analytics.summary(CurrentUser.from(jwt), from, to);
    }

    @Operation(summary = "Conversion funnel — distinct leads that ever reached each stage")
    @GetMapping("/api/analytics/funnel")
    public List<FunnelStage> funnel(@AuthenticationPrincipal Jwt jwt) {
        return analytics.funnel(CurrentUser.from(jwt));
    }

    @Operation(summary = "Drop-off — where and why leads stalled")
    @GetMapping("/api/analytics/drop-off")
    public List<DropOffRow> dropOff(@AuthenticationPrincipal Jwt jwt) {
        return analytics.dropOff(CurrentUser.from(jwt));
    }

    @Operation(summary = "Leads created vs converted over time")
    @GetMapping("/api/analytics/timeseries")
    public List<TimePoint> timeseries(@AuthenticationPrincipal Jwt jwt,
                                      @RequestParam(defaultValue = "week") String interval,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analytics.timeseries(CurrentUser.from(jwt), interval, from, to);
    }

    @Operation(summary = "Volume and conversion by channel")
    @GetMapping("/api/analytics/by-channel")
    public List<BreakdownRow> byChannel(@AuthenticationPrincipal Jwt jwt) {
        return analytics.breakdown(CurrentUser.from(jwt), "channels", "channel_id");
    }

    @Operation(summary = "Volume and conversion by course")
    @GetMapping("/api/analytics/by-course")
    public List<BreakdownRow> byCourse(@AuthenticationPrincipal Jwt jwt) {
        return analytics.breakdown(CurrentUser.from(jwt), "courses", "course_id");
    }

    @Operation(summary = "Volume and conversion by country of origin")
    @GetMapping("/api/analytics/by-country")
    public List<CountryRow> byCountry(@AuthenticationPrincipal Jwt jwt) {
        return analytics.byCountry(CurrentUser.from(jwt));
    }

    @Operation(summary = "Team leaderboard (admin only)")
    @GetMapping("/api/analytics/leaderboard")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public List<LeaderboardRow> leaderboard() {
        return analytics.leaderboard();
    }
}
