package com.example.resumescreener;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

// Every URL under /api/admin/** is ADMIN-only (see SecurityConfig)
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository users;

    public AdminController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/users")
    public List<UserView> listUsers() {
        return users.findAll(Sort.by("id")).stream().map(UserView::from).toList();
    }

    @PutMapping("/users/{id}/role")
    public UserView changeRole(@PathVariable Long id,
                               @RequestBody Map<String, String> body,
                               Authentication auth) {
        UserAccount target = loadOtherUser(id, auth);
        try {
            target.setRole(Role.valueOf(String.valueOf(body.get("role"))));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role must be USER or ADMIN");
        }
        return UserView.from(users.save(target));
    }

    @PutMapping("/users/{id}/enabled")
    public UserView setEnabled(@PathVariable Long id,
                               @RequestBody Map<String, Boolean> body,
                               Authentication auth) {
        UserAccount target = loadOtherUser(id, auth);
        Boolean enabled = body.get("enabled");
        if (enabled == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "'enabled' is required");
        }
        target.setEnabled(enabled);
        return UserView.from(users.save(target));
    }

    @DeleteMapping("/users/{id}")
    public Map<String, String> deleteUser(@PathVariable Long id, Authentication auth) {
        users.delete(loadOtherUser(id, auth));
        return Map.of("message", "User deleted");
    }

    // An admin cannot edit or delete their own account.
    // This guarantees the system can never end up with zero admins.
    private UserAccount loadOtherUser(Long id, Authentication auth) {
        UserAccount user = users.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getUsername().equals(auth.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot change your own account");
        }
        return user;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handle(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason()));
    }
}