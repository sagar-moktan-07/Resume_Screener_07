package com.example.resumescreener.config;

import com.example.resumescreener.entity.AdminUser;
import com.example.resumescreener.repository.AdminUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {
    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public DatabaseInitializer(AdminUserRepository adminUserRepository,
                              PasswordEncoder passwordEncoder,
                              @Value("${app.admin.username}") String adminUsername,
                              @Value("${app.admin.password}") String adminPassword) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        adminUserRepository.findByUsername(adminUsername)
                .orElseGet(() -> adminUserRepository.save(
                        new AdminUser(adminUsername, passwordEncoder.encode(adminPassword), "ADMIN")
                ));
    }
}
