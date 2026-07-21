package com.controlleads.activities;

import com.controlleads.common.CurrentUser;
import com.controlleads.leads.Lead;
import com.controlleads.leads.LeadRepository;
import com.controlleads.users.User;
import com.controlleads.users.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "activities")
public class ActivityController {

    public record ActivityDto(UUID id, UUID leadId, ActivityType type, String content,
                              Instant dueAt, Instant completedAt, UUID createdBy,
                              String createdByName, Instant createdAt) {}

    public record FollowUpDto(UUID id, UUID leadId, String leadName, ActivityType type,
                              String content, Instant dueAt, Instant completedAt, UUID createdBy,
                              String createdByName, Instant createdAt) {}

    public record CreateActivityRequest(@NotNull ActivityType type, @NotBlank String content,
                                        Instant dueAt) {}

    private final ActivityService activityService;
    private final UserRepository users;
    private final LeadRepository leads;

    public ActivityController(ActivityService activityService, UserRepository users,
                              LeadRepository leads) {
        this.activityService = activityService;
        this.users = users;
        this.leads = leads;
    }

    @Operation(summary = "Activities of a lead (ownership enforced)")
    @GetMapping("/api/leads/{id}/activities")
    public List<ActivityDto> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        List<Activity> list = activityService.listByLead(id, CurrentUser.from(jwt));
        Map<UUID, String> names = userNames(list.stream().map(Activity::getCreatedBy).toList());
        return list.stream().map(a -> toDto(a, names)).toList();
    }

    @Operation(summary = "Log an activity; contact kinds reset the lead's SLA clock")
    @PostMapping("/api/leads/{id}/activities")
    public ActivityDto add(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                           @Valid @RequestBody CreateActivityRequest request) {
        Activity activity = activityService.add(
            id, request.type(), request.content(), request.dueAt(), CurrentUser.from(jwt));
        return toDto(activity, userNames(List.of(activity.getCreatedBy())));
    }

    @Operation(summary = "Mark a follow-up as done")
    @PostMapping("/api/activities/{id}/complete")
    public ActivityDto complete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        Activity activity = activityService.complete(id, CurrentUser.from(jwt));
        return toDto(activity, userNames(List.of(activity.getCreatedBy())));
    }

    @Operation(summary = "My open follow-ups, ordered by due date")
    @GetMapping("/api/my/follow-ups")
    public List<FollowUpDto> myFollowUps(@AuthenticationPrincipal Jwt jwt) {
        List<Activity> list = activityService.myOpenFollowUps(CurrentUser.from(jwt));
        Map<UUID, String> names = userNames(list.stream().map(Activity::getCreatedBy).toList());
        Map<UUID, String> leadNames = leads.findAllById(list.stream().map(Activity::getLeadId).toList())
            .stream().collect(Collectors.toMap(Lead::getId, Lead::getFullName));
        return list.stream()
            .map(a -> new FollowUpDto(a.getId(), a.getLeadId(),
                leadNames.getOrDefault(a.getLeadId(), "?"), a.getType(), a.getContent(),
                a.getDueAt(), a.getCompletedAt(), a.getCreatedBy(),
                names.getOrDefault(a.getCreatedBy(), "?"), a.getCreatedAt()))
            .toList();
    }

    private Map<UUID, String> userNames(List<UUID> ids) {
        return users.findAllById(ids.stream().distinct().toList()).stream()
            .collect(Collectors.toMap(User::getId, User::getName));
    }

    private static ActivityDto toDto(Activity a, Map<UUID, String> names) {
        return new ActivityDto(a.getId(), a.getLeadId(), a.getType(), a.getContent(),
            a.getDueAt(), a.getCompletedAt(), a.getCreatedBy(),
            names.getOrDefault(a.getCreatedBy(), "?"), a.getCreatedAt());
    }
}
