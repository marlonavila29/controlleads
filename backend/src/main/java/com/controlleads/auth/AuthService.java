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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    public record TokenPair(String accessToken, String refreshToken, User user) {}

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final Duration refreshTtl;

    public AuthService(UserRepository users,
                       RefreshTokenRepository refreshTokens,
                       JwtService jwtService,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.security.jwt.refresh-ttl:14d}") Duration refreshTtl) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTtl = refreshTtl;
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

    private TokenPair issue(User user) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokens.save(new RefreshToken(user.getId(), sha256(raw), Instant.now().plus(refreshTtl)));
        return new TokenPair(jwtService.createAccessToken(user), raw, user);
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
