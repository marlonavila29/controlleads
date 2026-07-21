package com.controlleads.notifications;

import com.controlleads.common.CurrentUser;
import com.controlleads.leads.Lead;
import com.controlleads.leads.LeadRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "notifications")
public class NotificationController {

    public record NotificationDto(UUID id, NotificationType type, UUID leadId, String leadName,
                                  Instant readAt, Instant createdAt) {}

    public record UnreadCountDto(long count) {}

    private final NotificationService service;
    private final LeadRepository leads;

    public NotificationController(NotificationService service, LeadRepository leads) {
        this.service = service;
        this.leads = leads;
    }

    @Operation(summary = "My recent notifications (newest first)")
    @GetMapping("/api/notifications")
    public List<NotificationDto> recent(@AuthenticationPrincipal Jwt jwt) {
        CurrentUser caller = CurrentUser.from(jwt);
        List<Notification> list = service.recent(caller);
        Map<UUID, Lead> byId = leads.findAllById(
                list.stream().map(Notification::getLeadId).filter(Objects::nonNull).distinct().toList())
            .stream().collect(Collectors.toMap(Lead::getId, Function.identity()));
        return list.stream()
            .map(n -> new NotificationDto(n.getId(), n.getType(), n.getLeadId(),
                visibleLeadName(byId.get(n.getLeadId()), caller), n.getReadAt(), n.getCreatedAt()))
            .toList();
    }

    /**
     * Only expose the lead name if the caller may still see the lead — ownership
     * is mutable, so a notification held by a former owner must not leak the
     * lead's current name (or a since-deleted lead's name).
     */
    private static String visibleLeadName(Lead lead, CurrentUser caller) {
        if (lead == null || lead.getDeletedAt() != null) {
            return null;
        }
        return caller.isAdmin() || lead.getAssignedTo().equals(caller.id()) ? lead.getFullName() : null;
    }

    @Operation(summary = "Count of my unread notifications")
    @GetMapping("/api/notifications/unread-count")
    public UnreadCountDto unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return new UnreadCountDto(service.unreadCount(CurrentUser.from(jwt)));
    }

    @Operation(summary = "Mark one notification as read")
    @PostMapping("/api/notifications/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.markRead(id, CurrentUser.from(jwt));
    }

    @Operation(summary = "Mark all my notifications as read")
    @PostMapping("/api/notifications/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAllRead(@AuthenticationPrincipal Jwt jwt) {
        service.markAllRead(CurrentUser.from(jwt));
    }
}
