package com.example.resumescreener.dto;

import com.example.resumescreener.entity.Candidate;

import java.time.LocalDateTime;

/** Parsed data of one candidate, without the large raw text and without the owner's id. */
public record CandidateView(Long id, String fileName, String fullName, String email, String phone,
                            String qualifications, String skills, String experience,
                            String uploadedByUsername, LocalDateTime uploadedAt) {

    public static CandidateView from(Candidate c) {
        return new CandidateView(c.getId(), c.getFileName(), c.getFullName(), c.getEmail(),
                c.getPhone(), c.getQualifications(), c.getSkills(), c.getExperience(),
                c.getUploadedByUsername(), c.getUploadedAt());
    }
}