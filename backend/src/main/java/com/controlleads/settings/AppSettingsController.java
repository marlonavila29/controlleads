package com.controlleads.settings;

import com.controlleads.common.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "settings")
public class AppSettingsController {

    private final AppSettingsService settings;

    public AppSettingsController(AppSettingsService settings) {
        this.settings = settings;
    }

    public record SettingsDto(int hotLeadMaxHours, boolean shareLeadsVisibility) {}

    @Operation(summary = "Get system settings")
    @GetMapping("/api/settings")
    public SettingsDto getSettings() {
        return new SettingsDto(settings.hotLeadMaxHours(), settings.shareLeadsVisibility());
    }

    @Operation(summary = "Update system settings (admin only)")
    @PatchMapping("/api/settings")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public SettingsDto updateSettings(@AuthenticationPrincipal Jwt jwt, @RequestBody Map<String, Object> body) {
        CurrentUser caller = CurrentUser.from(jwt);

        if (body.containsKey("hotLeadMaxHours")) {
            Object val = body.get("hotLeadMaxHours");
            if (val instanceof Number) {
                settings.updateSetting("hot_lead_max_hours", String.valueOf(((Number) val).intValue()), caller.id());
            }
        }

        if (body.containsKey("shareLeadsVisibility")) {
            Object val = body.get("shareLeadsVisibility");
            if (val instanceof Boolean) {
                settings.updateSetting("share_leads_visibility", String.valueOf(val), caller.id());
            }
        }

        return getSettings();
    }
}
