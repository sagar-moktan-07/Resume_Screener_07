package com.example.resumescreener;

import java.time.LocalDateTime;

public record UserView(Long id, String username, String email, String fullName,
                       String role, boolean enabled, LocalDateTime createdAt) {

    public static UserView from(UserAccount u) {
        return new UserView(u.getId(), u.getUsername(), u.getEmail(), u.getFullName(),
                u.getRole().name(), u.isEnabled(), u.getCreatedAt());
    }
}