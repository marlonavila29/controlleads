package com.controlleads.users;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first ADMINISTRATOR when the users table is empty
 * (no self-signup exists — module_auth.md RN-03).
 * Override credentials via APP_SEED_ADMIN_* env vars; change the password after first login.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminSeeder(UserRepository users,
                       PasswordEncoder passwordEncoder,
                       @Value("${app.seed-admin.email:admin@controlleads.local}") String email,
                       @Value("${app.seed-admin.password:admin123}") String password) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!users.existsByEmailIgnoreCase(email)) {
            users.save(new User("Administrator", email, passwordEncoder.encode(password), UserRole.ADMINISTRATOR));
            log.warn("Seeded initial administrator '{}' — change this password after first login.", email);
        }
        String memberEmail = "member@controlleads.local";
        if (!users.existsByEmailIgnoreCase(memberEmail)) {
            users.save(new User("Sarah Counselor", memberEmail, passwordEncoder.encode("member123"), UserRole.MARKETING_TEAM));
            log.info("Seeded initial marketing team member '{}'.", memberEmail);
        }
    }
}
