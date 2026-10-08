package com.example.resumescreener.repository;

import com.example.resumescreener.entity.VacancyPost;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VacancyPostRepository extends JpaRepository<VacancyPost, Long> {
}
