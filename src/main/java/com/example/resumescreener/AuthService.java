package com.example.resumescreener;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9_]{3,30}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    public UserAccount register(SignupRequest req) {
        String username = normalize(req.username());
        String email = normalize(req.email());
        String fullName = req.fullName() == null ? "" : req.fullName().trim();
        String password = req.password() == null ? "" : req.password();

        if (!USERNAME.matcher(username).matches())
            throw new IllegalArgumentException("Username must be 3-30 characters: letters, numbers or underscore");
        if (email.length() > 254 || !EMAIL.matcher(email).matches())
            throw new IllegalArgumentException("Enter a valid email address");
        if (fullName.length() > 100)
            throw new IllegalArgumentException("Name is too long");
        if (password.length() < 8 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new IllegalArgumentException("Password must be at least 8 characters (72 bytes max)");
        if (users.existsByUsername(username))
            throw new IllegalArgumentException("Username is already taken");
        if (users.existsByEmail(email))
            throw new IllegalArgumentException("Email is already registered");

        // The role is NEVER read from the request. Everyone who signs up is a USER.
        return users.save(new UserAccount(username, email, fullName,
                encoder.encode(password), Role.USER));
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}