package com.example.resumescreener.config;

import com.example.resumescreener.repository.UserRepository;
import com.example.resumescreener.security.AccountStatusFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.SecurityContextHolderFilter;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UserRepository users) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public pages
                .requestMatchers(antMatcher("/login.html"), antMatcher("/signup.html"),
                                 antMatcher("/css/**"), antMatcher("/api/auth/signup")).permitAll()
                // Admin only
                .requestMatchers(antMatcher("/admin.html"), antMatcher("/api/admin/**")).hasRole("ADMIN")
                .requestMatchers(antMatcher(HttpMethod.DELETE, "/api/candidates/**")).hasRole("ADMIN")
                // Everything else: any logged-in user
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/index.html", true)
                .failureUrl("/login.html?error")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login.html?logout")
                .permitAll()
            )
            // API calls get a plain 401 instead of a redirect to the login page
            .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    antMatcher("/api/**")))
            // CSRF tokens are off to keep the static HTML pages simple.
            // The SameSite=Lax session cookie (application.properties) covers most of the risk.
            .csrf(csrf -> csrf.disable())
            .addFilterAfter(new AccountStatusFilter(users), SecurityContextHolderFilter.class);

        return http.build();
    }
}
