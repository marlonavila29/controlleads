package com.controlleads.auth;

import com.controlleads.users.UserController.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "auth")
public class AuthController {

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record RefreshRequest(@NotBlank String refreshToken) {}
    public record AuthResponse(String accessToken, String refreshToken, UserDto user) {}

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Authenticate with email/password, returns JWT access + rotating refresh token")
    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return toResponse(authService.login(request.email(), request.password()));
    }

    @Operation(summary = "Exchange a refresh token for a new token pair (old one is revoked)")
    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody RefreshRequest request) {
        return toResponse(authService.refresh(request.refreshToken()));
    }

    private static AuthResponse toResponse(AuthService.TokenPair pair) {
        return new AuthResponse(pair.accessToken(), pair.refreshToken(), UserDto.from(pair.user()));
    }
}
