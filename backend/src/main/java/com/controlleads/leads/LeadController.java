package com.controlleads.leads;

import com.controlleads.common.CurrentUser;
import com.controlleads.users.User;
import com.controlleads.users.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "leads")
public class LeadController {

    public record LeadDto(UUID id, String fullName, String countryCode, String email, String phone,
                          UUID courseId, UUID channelId, String utmSource, String utmMedium,
                          String utmCampaign, LeadStatus status, LeadStatus stalledFromStatus,
                          UUID stallReasonId, UUID assignedTo, String assignedToName,
                          Instant lastContactedAt, Instant createdAt) {}

    public record StatusEventDto(UUID id, LeadStatus fromStatus, LeadStatus toStatus,
                                 UUID stallReasonId, String note, UUID changedBy,
                                 String changedByName, Instant changedAt) {}

    public record LeadDetailDto(LeadDto lead, List<StatusEventDto> statusHistory) {}

    public record LeadPageDto(List<LeadDto> content, long totalElements, int totalPages, int page) {}

    public record DuplicateDto(UUID id, String fullName, LeadStatus status, String assignedToName) {}

    public record CreateLeadRequest(@NotBlank String fullName,
                                    @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String countryCode,
                                    String email, String phone,
                                    @NotNull UUID courseId, @NotNull UUID channelId,
                                    UUID assignedTo,
                                    String utmSource, String utmMedium, String utmCampaign) {}

    public record UpdateLeadRequest(String fullName, @Pattern(regexp = "[A-Za-z]{2}") String countryCode,
                                    String email, String phone, UUID courseId, UUID channelId,
                                    UUID assignedTo) {}

    public record TransitionRequest(@NotNull LeadStatus toStatus, UUID stallReasonId, String note) {}

    private final LeadService leadService;
    private final LeadRepository leads;
    private final LeadStatusEventRepository statusEvents;
    private final UserRepository users;

    public LeadController(LeadService leadService, LeadRepository leads,
                          LeadStatusEventRepository statusEvents, UserRepository users) {
        this.leadService = leadService;
        this.leads = leads;
        this.statusEvents = statusEvents;
        this.users = users;
    }

    @Operation(summary = "Create a lead (owner defaults to the caller; admins may assign)")
    @PostMapping("/api/leads")
    @ResponseStatus(HttpStatus.CREATED)
    public LeadDto create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateLeadRequest request) {
        Lead lead = leadService.create(request, CurrentUser.from(jwt));
        return toDto(lead, userNames(List.of(lead.getAssignedTo())));
    }

    @Operation(summary = "Update lead data; reassignment is admin-only and audited")
    @PatchMapping("/api/leads/{id}")
    public LeadDto update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                          @Valid @RequestBody UpdateLeadRequest request) {
        Lead lead = leadService.update(id, request, CurrentUser.from(jwt));
        return toDto(lead, userNames(List.of(lead.getAssignedTo())));
    }

    @Operation(summary = "Move a lead through the funnel; every accepted move writes an immutable event")
    @PostMapping("/api/leads/{id}/transition")
    public LeadDto transition(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                              @Valid @RequestBody TransitionRequest request) {
        Lead lead = leadService.transition(id, request, CurrentUser.from(jwt));
        return toDto(lead, userNames(List.of(lead.getAssignedTo())));
    }

    @Operation(summary = "Lead detail with full status history")
    @GetMapping("/api/leads/{id}")
    public LeadDetailDto detail(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        Lead lead = leadService.loadVisible(id, CurrentUser.from(jwt));
        List<LeadStatusEvent> history = statusEvents.findByLeadIdOrderByChangedAtAsc(id);

        List<UUID> userIds = new ArrayList<>(history.stream().map(LeadStatusEvent::getChangedBy).toList());
        userIds.add(lead.getAssignedTo());
        Map<UUID, String> names = userNames(userIds);

        List<StatusEventDto> events = history.stream()
            .map(e -> new StatusEventDto(e.getId(), e.getFromStatus(), e.getToStatus(),
                e.getStallReasonId(), e.getNote(), e.getChangedBy(),
                names.getOrDefault(e.getChangedBy(), "?"), e.getChangedAt()))
            .toList();
        return new LeadDetailDto(toDto(lead, names), events);
    }

    @Operation(summary = "Search/filter leads (marketing sees own; admins see all)")
    @GetMapping("/api/leads")
    public LeadPageDto list(@AuthenticationPrincipal Jwt jwt,
                            @RequestParam(required = false) LeadStatus status,
                            @RequestParam(required = false) String countryCode,
                            @RequestParam(required = false) UUID courseId,
                            @RequestParam(required = false) UUID channelId,
                            @RequestParam(required = false) UUID assignedTo,
                            @RequestParam(required = false) String q,
                            @RequestParam(required = false) LocalDate createdFrom,
                            @RequestParam(required = false) LocalDate createdTo,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "25") int size) {
        CurrentUser caller = CurrentUser.from(jwt);

        Specification<Lead> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (!caller.isAdmin()) {
                predicates.add(cb.equal(root.get("assignedTo"), caller.id()));
            } else if (assignedTo != null) {
                predicates.add(cb.equal(root.get("assignedTo"), assignedTo));
            }
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (countryCode != null) predicates.add(cb.equal(root.get("countryCode"), countryCode.toUpperCase()));
            if (courseId != null) predicates.add(cb.equal(root.get("courseId"), courseId));
            if (channelId != null) predicates.add(cb.equal(root.get("channelId"), channelId));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("fullName")), like),
                    cb.like(cb.lower(root.get("email")), like),
                    cb.like(root.get("phone"), "%" + q.trim() + "%")));
            }
            if (createdFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"),
                    createdFrom.atStartOfDay().toInstant(ZoneOffset.UTC)));
            }
            if (createdTo != null) {
                predicates.add(cb.lessThan(root.get("createdAt"),
                    createdTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        Page<Lead> result = leads.findAll(spec,
            PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<UUID, String> names = userNames(result.map(Lead::getAssignedTo).toList());
        return new LeadPageDto(result.map(l -> toDto(l, names)).getContent(),
            result.getTotalElements(), result.getTotalPages(), result.getNumber());
    }

    @Operation(summary = "Possible duplicates by email/phone — warn, don't block (RN-04)")
    @GetMapping("/api/leads/duplicates")
    public List<DuplicateDto> duplicates(@RequestParam(required = false) String email,
                                         @RequestParam(required = false) String phone) {
        List<Lead> matches = new ArrayList<>();
        if (email != null && !email.isBlank()) {
            matches.addAll(leads.findByEmailIgnoreCaseAndDeletedAtIsNull(email.trim()));
        }
        if (phone != null && !phone.isBlank()) {
            matches.addAll(leads.findByPhoneAndDeletedAtIsNull(phone.trim()));
        }
        Map<UUID, String> names = userNames(matches.stream().map(Lead::getAssignedTo).toList());
        return matches.stream()
            .collect(Collectors.toMap(Lead::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new))
            .values().stream()
            .map(l -> new DuplicateDto(l.getId(), l.getFullName(), l.getStatus(),
                names.getOrDefault(l.getAssignedTo(), "?")))
            .toList();
    }

    private Map<UUID, String> userNames(List<UUID> ids) {
        return users.findAllById(ids.stream().distinct().toList()).stream()
            .collect(Collectors.toMap(User::getId, User::getName));
    }

    private static LeadDto toDto(Lead lead, Map<UUID, String> names) {
        return new LeadDto(lead.getId(), lead.getFullName(), lead.getCountryCode(), lead.getEmail(),
            lead.getPhone(), lead.getCourseId(), lead.getChannelId(), lead.getUtmSource(),
            lead.getUtmMedium(), lead.getUtmCampaign(), lead.getStatus(), lead.getStalledFromStatus(),
            lead.getStallReasonId(), lead.getAssignedTo(),
            names.getOrDefault(lead.getAssignedTo(), "?"), lead.getLastContactedAt(), lead.getCreatedAt());
    }
}
