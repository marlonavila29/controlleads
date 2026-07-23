package com.controlleads.communications;

import com.controlleads.common.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "communications")
public class CommunicationController {

    private final CommunicationService communicationService;

    public CommunicationController(CommunicationService communicationService) {
        this.communicationService = communicationService;
    }

    public record LogDto(UUID id, UUID leadId, String leadName, String sentByName, String channel,
                         String recipientAddress, String subject, String body, String status, Instant createdAt) {
        public static LogDto from(CommunicationLog log) {
            return new LogDto(
                log.getId(),
                log.getLead().getId(),
                log.getLead().getFullName(),
                log.getSentBy().getName(),
                log.getChannel(),
                log.getRecipientAddress(),
                log.getSubject(),
                log.getBody(),
                log.getStatus(),
                log.getCreatedAt()
            );
        }
    }

    @Operation(summary = "Send email or WhatsApp communications in bulk to selected candidates")
    @PostMapping("/api/communications/send")
    public CommunicationService.SendResultDto sendBatch(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CommunicationService.SendBatchRequest request) {
        return communicationService.sendBatch(request, CurrentUser.from(jwt));
    }

    @Operation(summary = "Get global logs of all sent communications")
    @GetMapping("/api/communications/logs")
    public List<LogDto> getAllLogs() {
        return communicationService.getAllLogs().stream()
            .map(LogDto::from)
            .toList();
    }

    @Operation(summary = "Get all communications sent to a specific lead")
    @GetMapping("/api/communications/logs/lead/{leadId}")
    public List<LogDto> getLeadLogs(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID leadId) {
        return communicationService.getLogsForLead(leadId, CurrentUser.from(jwt)).stream()
            .map(LogDto::from)
            .toList();
    }
}
