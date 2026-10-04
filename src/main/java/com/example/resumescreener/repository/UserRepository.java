package com.example.resumescreener.repository;

import com.example.resumescreener.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
