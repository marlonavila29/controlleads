package com.controlleads.settings;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Typed access to the app_settings key/value table. Single home for defaults
 * so Analytics and Notifications agree on the SLA threshold.
 */
@Service
public class AppSettingsService {

    public static final int DEFAULT_HOT_LEAD_MAX_HOURS = 24;

    private final NamedParameterJdbcTemplate jdbc;

    public AppSettingsService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Hours a HOT_LEAD may go uncontacted before it breaches SLA. */
    public int hotLeadMaxHours() {
        try {
            Integer hours = jdbc.queryForObject(
                "SELECT (value #>> '{}')::int FROM app_settings WHERE key = 'hot_lead_max_hours'",
                new MapSqlParameterSource(), Integer.class);
            return hours == null ? DEFAULT_HOT_LEAD_MAX_HOURS : hours;
        } catch (RuntimeException e) {
            return DEFAULT_HOT_LEAD_MAX_HOURS;
        }
    }
}
