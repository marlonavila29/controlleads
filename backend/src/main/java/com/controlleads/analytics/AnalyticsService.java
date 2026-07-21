package com.controlleads.analytics;

import com.controlleads.analytics.AnalyticsController.BreakdownRow;
import com.controlleads.analytics.AnalyticsController.CountryRow;
import com.controlleads.analytics.AnalyticsController.DropOffRow;
import com.controlleads.analytics.AnalyticsController.FunnelStage;
import com.controlleads.analytics.AnalyticsController.LeaderboardRow;
import com.controlleads.analytics.AnalyticsController.Summary;
import com.controlleads.analytics.AnalyticsController.TimePoint;
import com.controlleads.common.ApiException;
import com.controlleads.common.CurrentUser;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * All aggregations derive from lead_status_events (never leads.status), which
 * makes the funnel/conversion/drop-off numbers reproducible and immune to
 * later status corrections. Every query is scoped to the caller's own leads
 * unless the caller is an administrator.
 */
@Service
public class AnalyticsService {

    private static final List<String> FUNNEL_ORDER =
        List.of("LEAD", "HOT_LEAD", "APPLICATION", "STUDENT");
    private static final Set<String> VALID_INTERVALS = Set.of("week", "month");
    private static final Map<String, String> BREAKDOWN_COLUMNS =
        Map.of("channels", "channel_id", "courses", "course_id");

    private final NamedParameterJdbcTemplate jdbc;

    public AnalyticsService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---- summary -------------------------------------------------------------

    public Summary summary(CurrentUser caller, LocalDate from, LocalDate to) {
        String scope = scope(caller);
        MapSqlParameterSource params = base(caller);
        OffsetDateTime[] window = window(from, to, 30);
        params.addValue("from", window[0]);
        params.addValue("toExcl", window[1]);
        params.addValue("hotHours", hotLeadMaxHours());

        long total = count("SELECT count(*) FROM leads l WHERE l.deleted_at IS NULL" + scope, params);
        long newLeads = count("SELECT count(*) FROM leads l WHERE l.deleted_at IS NULL" + scope
            + " AND l.created_at >= :from AND l.created_at < :toExcl", params);
        long students = count("""
            SELECT count(DISTINCT e.lead_id) FROM lead_status_events e
            JOIN leads l ON l.id = e.lead_id
            WHERE l.deleted_at IS NULL AND e.to_status = 'STUDENT'""" + scope, params);

        Double avgDays = jdbc.queryForObject("""
            SELECT avg(EXTRACT(EPOCH FROM (x.first_student - l.created_at)) / 86400.0)
            FROM (SELECT e.lead_id, min(e.changed_at) AS first_student
                  FROM lead_status_events e WHERE e.to_status = 'STUDENT'
                  GROUP BY e.lead_id) x
            JOIN leads l ON l.id = x.lead_id
            WHERE l.deleted_at IS NULL""" + scope, params, Double.class);

        long slaBreaches = count("""
            SELECT count(*) FROM leads l
            WHERE l.deleted_at IS NULL AND l.status = 'HOT_LEAD'
              AND (l.last_contacted_at IS NULL
                   OR l.last_contacted_at < now() - (:hotHours * interval '1 hour'))""" + scope, params);

        double conversion = total == 0 ? 0.0 : (double) students / total;
        return new Summary(total, newLeads, conversion, avgDays, slaBreaches);
    }

    // ---- funnel (event-derived, ever reached) --------------------------------

    public List<FunnelStage> funnel(CurrentUser caller) {
        Map<String, Long> counts = new LinkedHashMap<>();
        jdbc.query("""
            SELECT e.to_status AS status, count(DISTINCT e.lead_id) AS c
            FROM lead_status_events e
            JOIN leads l ON l.id = e.lead_id
            WHERE l.deleted_at IS NULL
              AND e.to_status IN ('LEAD','HOT_LEAD','APPLICATION','STUDENT')""" + scope(caller)
            + " GROUP BY e.to_status", base(caller),
            rs -> { counts.put(rs.getString("status"), rs.getLong("c")); });

        return FUNNEL_ORDER.stream()
            .map(status -> new FunnelStage(status, counts.getOrDefault(status, 0L)))
            .toList();
    }

    // ---- drop-off ------------------------------------------------------------

    public List<DropOffRow> dropOff(CurrentUser caller) {
        // count(DISTINCT lead_id) so a lead that stalled, was revived, then
        // stalled again at the same stage/reason still counts once — consistent
        // with every other distinct-lead metric in this service.
        return jdbc.query("""
            SELECT e.from_status AS stage, e.stall_reason_id AS reason_id,
                   sr.name AS reason_name, count(DISTINCT e.lead_id) AS c
            FROM lead_status_events e
            JOIN leads l ON l.id = e.lead_id
            LEFT JOIN stall_reasons sr ON sr.id = e.stall_reason_id
            WHERE l.deleted_at IS NULL AND e.to_status = 'STALLED'""" + scope(caller)
            + " GROUP BY e.from_status, e.stall_reason_id, sr.name ORDER BY c DESC",
            base(caller),
            (rs, i) -> new DropOffRow(rs.getString("stage"),
                rs.getObject("reason_id", UUID.class),
                rs.getString("reason_name") == null ? "Unspecified" : rs.getString("reason_name"),
                rs.getLong("c")));
    }

    // ---- timeseries ----------------------------------------------------------

    public List<TimePoint> timeseries(CurrentUser caller, String interval, LocalDate from, LocalDate to) {
        if (!VALID_INTERVALS.contains(interval)) {
            throw ApiException.badRequest("interval must be 'week' or 'month'");
        }
        String scope = scope(caller);
        OffsetDateTime[] window = window(from, to, "month".equals(interval) ? 365 : 84);
        MapSqlParameterSource params = base(caller);
        params.addValue("interval", interval);
        params.addValue("from", window[0]);
        params.addValue("toExcl", window[1]);

        Map<String, long[]> byPeriod = new LinkedHashMap<>();
        // Truncate in UTC so bucket boundaries line up with the UTC [from,toExcl)
        // window regardless of the DB session timezone.
        jdbc.query("""
            SELECT date_trunc(:interval, l.created_at AT TIME ZONE 'UTC')::date AS period, count(*) AS c
            FROM leads l WHERE l.deleted_at IS NULL
              AND l.created_at >= :from AND l.created_at < :toExcl""" + scope
            + " GROUP BY period", params,
            rs -> { byPeriod.computeIfAbsent(rs.getString("period"), k -> new long[2])[0] = rs.getLong("c"); });

        jdbc.query("""
            SELECT date_trunc(:interval, x.first_student AT TIME ZONE 'UTC')::date AS period, count(*) AS c
            FROM (SELECT e.lead_id, min(e.changed_at) AS first_student
                  FROM lead_status_events e
                  JOIN leads l ON l.id = e.lead_id
                  WHERE l.deleted_at IS NULL AND e.to_status = 'STUDENT'""" + scope + """
                  GROUP BY e.lead_id) x
            WHERE x.first_student >= :from AND x.first_student < :toExcl
            GROUP BY period""", params,
            rs -> { byPeriod.computeIfAbsent(rs.getString("period"), k -> new long[2])[1] = rs.getLong("c"); });

        Set<String> periods = new TreeSet<>(byPeriod.keySet());
        List<TimePoint> out = new ArrayList<>();
        for (String period : periods) {
            long[] v = byPeriod.get(period);
            out.add(new TimePoint(period, v[0], v[1]));
        }
        return out;
    }

    // ---- breakdowns ----------------------------------------------------------

    public List<BreakdownRow> breakdown(CurrentUser caller, String table, String column) {
        if (!BREAKDOWN_COLUMNS.getOrDefault(table, "").equals(column)) {
            throw new IllegalArgumentException("Unsupported breakdown: " + table);
        }
        String sql = ("""
            SELECT cat.id AS id, cat.name AS name,
                   count(l.id) AS total,
                   count(DISTINCT s.lead_id) AS students
            FROM leads l
            JOIN %s cat ON cat.id = l.%s
            LEFT JOIN (SELECT DISTINCT lead_id FROM lead_status_events WHERE to_status = 'STUDENT') s
                   ON s.lead_id = l.id
            WHERE l.deleted_at IS NULL""").formatted(table, column) + scope(caller)
            + " GROUP BY cat.id, cat.name ORDER BY total DESC";
        return jdbc.query(sql, base(caller),
            (rs, i) -> new BreakdownRow(rs.getObject("id", UUID.class), rs.getString("name"),
                rs.getLong("total"), rs.getLong("students")));
    }

    public List<CountryRow> byCountry(CurrentUser caller) {
        return jdbc.query("""
            SELECT l.country_code AS country_code,
                   count(*) AS total,
                   count(DISTINCT s.lead_id) AS students
            FROM leads l
            LEFT JOIN (SELECT DISTINCT lead_id FROM lead_status_events WHERE to_status = 'STUDENT') s
                   ON s.lead_id = l.id
            WHERE l.deleted_at IS NULL""" + scope(caller)
            + " GROUP BY l.country_code ORDER BY total DESC",
            base(caller),
            (rs, i) -> new CountryRow(rs.getString("country_code"),
                rs.getLong("total"), rs.getLong("students")));
    }

    // ---- leaderboard (admin) -------------------------------------------------

    public List<LeaderboardRow> leaderboard() {
        return jdbc.query("""
            SELECT u.id AS id, u.name AS name,
                   count(l.id) AS total_leads,
                   count(DISTINCT s.lead_id) AS students
            FROM users u
            LEFT JOIN leads l ON l.assigned_to = u.id AND l.deleted_at IS NULL
            LEFT JOIN (SELECT DISTINCT lead_id FROM lead_status_events WHERE to_status = 'STUDENT') s
                   ON s.lead_id = l.id
            GROUP BY u.id, u.name
            ORDER BY students DESC, total_leads DESC""",
            new MapSqlParameterSource(),
            (rs, i) -> new LeaderboardRow(rs.getObject("id", UUID.class), rs.getString("name"),
                rs.getLong("total_leads"), rs.getLong("students")));
    }

    // ---- helpers -------------------------------------------------------------

    /** Ownership filter fragment; leads must be aliased `l`. */
    private String scope(CurrentUser caller) {
        return caller.isAdmin() ? "" : " AND l.assigned_to = :userId";
    }

    private MapSqlParameterSource base(CurrentUser caller) {
        return new MapSqlParameterSource("userId", caller.id());
    }

    private long count(String sql, MapSqlParameterSource params) {
        Long value = jdbc.queryForObject(sql, params, Long.class);
        return value == null ? 0L : value;
    }

    private int hotLeadMaxHours() {
        try {
            Integer hours = jdbc.queryForObject(
                "SELECT (value #>> '{}')::int FROM app_settings WHERE key = 'hot_lead_max_hours'",
                new MapSqlParameterSource(), Integer.class);
            return hours == null ? 24 : hours;
        } catch (RuntimeException e) {
            return 24;
        }
    }

    /**
     * Resolve an optional [from,to] date filter into a half-open UTC instant
     * window. Missing bounds fall back to (now - defaultLookbackDays, now).
     */
    private OffsetDateTime[] window(LocalDate from, LocalDate to, int defaultLookbackDays) {
        OffsetDateTime end = to != null
            ? to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC)
            : OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime start = from != null
            ? from.atStartOfDay().atOffset(ZoneOffset.UTC)
            : end.minusDays(defaultLookbackDays);
        return new OffsetDateTime[] { start, end };
    }
}
