package com.controlleads.settings;

import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Typed access to the app_settings key/value table.
 */
@Service
public class AppSettingsService {

    public static final int DEFAULT_HOT_LEAD_MAX_HOURS = 24;
    public static final boolean DEFAULT_SHARE_LEADS_VISIBILITY = false;

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

    /** Whether marketing team members can see leads assigned to others. */
    public boolean shareLeadsVisibility() {
        try {
            Boolean share = jdbc.queryForObject(
                "SELECT (value #>> '{}')::boolean FROM app_settings WHERE key = 'share_leads_visibility'",
                new MapSqlParameterSource(), Boolean.class);
            return share == null ? DEFAULT_SHARE_LEADS_VISIBILITY : share;
        } catch (RuntimeException e) {
            return DEFAULT_SHARE_LEADS_VISIBILITY;
        }
    }

    @Transactional
    public void updateSetting(String key, String jsonValue, UUID updatedBy) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("key", key);
        params.addValue("value", jsonValue);
        params.addValue("updatedBy", updatedBy);
        jdbc.update("""
            INSERT INTO app_settings (key, value, updated_by, updated_at)
            VALUES (:key, cast(:value as jsonb), :updatedBy, now())
            ON CONFLICT (key) DO UPDATE
            SET value = cast(:value as jsonb), updated_by = :updatedBy, updated_at = now()
            """, params);
    }
}
