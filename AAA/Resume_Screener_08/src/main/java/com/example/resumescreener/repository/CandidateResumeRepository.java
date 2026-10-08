package com.example.resumescreener.repository;

import com.example.resumescreener.entity.CandidateResume;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CandidateResumeRepository extends JpaRepository<CandidateResume, Long> {
}
