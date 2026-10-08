package com.example.resumescreener.repository;

import com.example.resumescreener.entity.ScreeningResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreeningResultRepository extends JpaRepository<ScreeningResult, Long> {
}
