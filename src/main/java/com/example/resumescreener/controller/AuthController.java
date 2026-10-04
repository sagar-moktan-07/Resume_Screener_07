package com.example.resumescreener.controller;

import com.example.resumescreener.dto.SignupRequest;
import com.example.resumescreener.dto.UserView;
import com.example.resumescreener.repository.UserRepository;
import com.example.resumescreener.service.AuthService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository users;

    public AuthController(AuthService authService, UserRepository users) {
        this.authService = authService;
        this.users = users;
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, String>> signup(@RequestBody SignupRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Account created. You can log in now."));
    }

    // Used by the frontend to show the user's name and hide admin-only buttons
    @GetMapping("/me")
    public UserView me(Authentication auth) {
        return users.findByUsername(auth.getName()).map(UserView::from).orElseThrow();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> duplicate(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", "Username or email already exists"));
    }
}
