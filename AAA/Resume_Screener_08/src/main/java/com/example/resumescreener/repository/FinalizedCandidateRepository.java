package com.example.resumescreener.repository;

import com.example.resumescreener.entity.FinalizedCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinalizedCandidateRepository extends JpaRepository<FinalizedCandidate, Long> {
}
