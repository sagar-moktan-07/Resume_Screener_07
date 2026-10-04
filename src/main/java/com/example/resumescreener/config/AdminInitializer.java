package com.example.resumescreener.config;

import com.example.resumescreener.entity.Role;
import com.example.resumescreener.entity.UserAccount;
import com.example.resumescreener.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInitializer.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Value("${app.admin.username}")
    private String username;

    @Value("${app.admin.email}")
    private String email;

    @Value("${app.admin.password}")
    private String password;

    public AdminInitializer(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        String name = username.trim().toLowerCase();
        if (users.existsByUsername(name)) return;     // already created on an earlier run

        users.save(new UserAccount(name, email.trim().toLowerCase(),
                "Administrator", encoder.encode(password), Role.ADMIN));

        log.warn("Admin account '{}' created. Make sure the password is not the default!", name);
    }
}
