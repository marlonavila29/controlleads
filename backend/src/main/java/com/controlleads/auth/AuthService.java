package com.controlleads.auth;

import com.controlleads.common.ApiException;
import com.controlleads.users.User;
import com.controlleads.users.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    public record TokenPair(String accessToken, String refreshToken, User user) {}

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordResetTokenRepository resetTokens;
    private final ApplicationEventPublisher events;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final Duration refreshTtl;
    private final Duration resetTtl;

    public AuthService(UserRepository users,
                       RefreshTokenRepository refreshTokens,
                       PasswordResetTokenRepository resetTokens,
                       ApplicationEventPublisher events,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.security.jwt.refresh-ttl:14d}") Duration refreshTtl,
                       @Value("${app.security.password-reset-ttl:1h}") Duration resetTtl) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.resetTokens = resetTokens;
        this.events = events;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTtl = refreshTtl;
        this.resetTtl = resetTtl;
    }

    @Transactional
    public TokenPair login(String email, String password) {
        User user = users.findByEmailIgnoreCase(email)
            .filter(User::isActive)
            .orElseThrow(() -> ApiException.unauthorized("Invalid credentials"));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid credentials");
        }
        return issue(user);
    }

    /** Rotating refresh: the presented token is revoked and a new pair issued. */
    @Transactional
    public TokenPair refresh(String rawRefreshToken) {
        RefreshToken stored = refreshTokens.findByTokenHash(sha256(rawRefreshToken))
            .filter(RefreshToken::isUsable)
            .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
        User user = users.findById(stored.getUserId())
            .filter(User::isActive)
            .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));
        stored.revoke();
        return issue(user);
    }

    /**
     * Start a password reset. Always succeeds silently — we never reveal whether
     * an email is registered. A single-use token is stored (hashed) and the raw
     * value is handed to the mailer.
     */
    @Transactional
    public void forgotPassword(String email) {
        users.findByEmailIgnoreCase(email)
            .filter(User::isActive)
            .ifPresent(user -> {
                String raw = randomToken();
                resetTokens.save(new PasswordResetToken(
                    user.getId(), sha256(raw), Instant.now().plus(resetTtl)));
                // Emailed after commit, asynchronously — see PasswordResetDispatchListener.
                events.publishEvent(new PasswordResetRequestedEvent(user.getEmail(), raw));
            });
    }

    /**
     * Complete a password reset: consume the token, set the new password, and
     * revoke every existing session so a stolen refresh token can't outlive it.
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = resetTokens.findByTokenHash(sha256(rawToken))
            .filter(PasswordResetToken::isUsable)
            .orElseThrow(() -> ApiException.badRequest("Invalid or expired reset token"));
        User user = users.findById(token.getUserId())
            .orElseThrow(() -> ApiException.badRequest("Invalid or expired reset token"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        token.markUsed();
        refreshTokens.revokeAllForUser(user.getId(), Instant.now());
    }

    private TokenPair issue(User user) {
        String raw = randomToken();
        refreshTokens.save(new RefreshToken(user.getId(), sha256(raw), Instant.now().plus(refreshTtl)));
        return new TokenPair(jwtService.createAccessToken(user), raw, user);
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
