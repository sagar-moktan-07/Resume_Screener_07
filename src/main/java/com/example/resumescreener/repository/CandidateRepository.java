package com.example.resumescreener.repository;

import com.example.resumescreener.entity.Candidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CandidateRepository extends JpaRepository<Candidate, Long> {

    // A normal user only ever gets their own rows
    List<Candidate> findByUploadedByIdOrderByIdDesc(Long uploadedById);
}