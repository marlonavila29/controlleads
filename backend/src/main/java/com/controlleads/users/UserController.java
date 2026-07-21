package com.controlleads.users;

import com.controlleads.common.ApiException;
import com.controlleads.common.CurrentUser;
import com.controlleads.leads.LeadRepository;
import com.controlleads.leads.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

@RestController
@Tag(name = "users")
public class UserController {

    public record UserDto(UUID id, String name, String email, UserRole role, boolean active, Instant createdAt) {
        public static UserDto from(User user) {
            return new UserDto(user.getId(), user.getName(), user.getEmail(),
                user.getRole(), user.isActive(), user.getCreatedAt());
        }
    }

    public record TeamMemberDto(UUID id, String name, String email, UserRole role, boolean active,
                                Instant createdAt, long leadCount) {}

    public record CreateUserRequest(@NotBlank String name, @NotBlank @Email String email,
                                    @NotBlank @Size(min = 8) String password, @NotNull UserRole role) {}
    public record UpdateUserRequest(String name, UserRole role, Boolean active) {}
    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Size(min = 8) String newPassword) {}
    public record ReassignLeadsRequest(@NotNull UUID toUserId) {}
    public record ReassignResultDto(int reassigned) {}

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final LeadRepository leads;
    private final LeadService leadService;

    public UserController(UserRepository users, PasswordEncoder passwordEncoder,
                          LeadRepository leads, LeadService leadService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.leads = leads;
        this.leadService = leadService;
    }

    // ----- Admin management (module_auth.md RF-02/RF-03) -----

    @Operation(summary = "List all team members with their active-lead counts (admin only)")
    @GetMapping("/api/users")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public List<TeamMemberDto> list() {
        return users.findAll().stream()
            .map(u -> new TeamMemberDto(u.getId(), u.getName(), u.getEmail(), u.getRole(),
                u.isActive(), u.getCreatedAt(), leads.countByAssignedToAndDeletedAtIsNull(u.getId())))
            .toList();
    }

    @Operation(summary = "Reassign all of a user's active leads to another user (admin only; RN-02)")
    @PostMapping("/api/users/{id}/reassign-leads")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ReassignResultDto reassignLeads(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                           @Valid @RequestBody ReassignLeadsRequest request) {
        int count = leadService.reassignAll(id, request.toUserId(), CurrentUser.from(jwt));
        return new ReassignResultDto(count);
    }

    @Operation(summary = "Create a team member (admin only)")
    @PostMapping("/api/users")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto create(@Valid @RequestBody CreateUserRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw ApiException.conflict("A user with this email already exists");
        }
        User user = new User(request.name(), request.email(),
            passwordEncoder.encode(request.password()), request.role());
        return UserDto.from(users.save(user));
    }

    @Operation(summary = "Update name, role or active flag (admin only; users are deactivated, never deleted)")
    @PatchMapping("/api/users/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Transactional
    public UserDto update(@PathVariable UUID id, @RequestBody UpdateUserRequest request) {
        User user = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found"));
        if (request.name() != null) user.setName(request.name());
        if (request.role() != null) user.setRole(request.role());
        if (request.active() != null) user.setActive(request.active());
        return UserDto.from(user);
    }

    // ----- Own profile (module_auth.md RF-05) -----

    @Operation(summary = "Current authenticated user")
    @GetMapping("/api/me")
    public UserDto me(@AuthenticationPrincipal Jwt jwt) {
        return UserDto.from(current(jwt));
    }

    @Operation(summary = "Change own password")
    @PostMapping("/api/me/password")
    @Transactional
    public void changePassword(@AuthenticationPrincipal Jwt jwt,
                               @Valid @RequestBody ChangePasswordRequest request) {
        User user = current(jwt);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw ApiException.badRequest("Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private User current(Jwt jwt) {
        return users.findById(UUID.fromString(jwt.getSubject()))
            .orElseThrow(() -> ApiException.unauthorized("Unknown user"));
    }
}
