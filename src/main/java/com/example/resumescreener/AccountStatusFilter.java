package com.example.resumescreener;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Runs on every request. If the logged-in account was disabled, deleted,
 * or had its role changed by an admin, the session is dropped right away.
 * Without this, a disabled user would stay logged in until their session expired.
 */
public class AccountStatusFilter extends OncePerRequestFilter {

    private final UserRepository users;

    public AccountStatusFilter(UserRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            Optional<UserAccount> account = users.findByUsername(auth.getName());

            boolean stillValid = account.isPresent()
                    && account.get().isEnabled()
                    && auth.getAuthorities().stream().anyMatch(
                            a -> a.getAuthority().equals("ROLE_" + account.get().getRole().name()));

            if (!stillValid) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) session.invalidate();
            }
        }

        chain.doFilter(request, response);
    }
}