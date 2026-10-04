package com.example.resumescreener.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "credentials_all",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_credentials_username", columnNames = "username"),
           @UniqueConstraint(name = "uk_credentials_email", columnNames = "email")
       })
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(name = "full_name", length = 100)
    private String fullName;

    // BCrypt hash, never the real password
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected UserAccount() {}

    public UserAccount(String username, String email, String fullName,
                       String passwordHash, Role role) {
        this.username = username;
        this.email = email;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
    public boolean isEnabled() { return enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setRole(Role role) { this.role = role; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
