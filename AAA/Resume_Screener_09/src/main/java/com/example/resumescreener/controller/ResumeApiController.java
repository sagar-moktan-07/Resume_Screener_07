package com.example.resumescreener.controller;

import com.example.resumescreener.entity.CandidateResume;
import com.example.resumescreener.entity.UserCredentials;
import com.example.resumescreener.entity.VacancyPost;
import com.example.resumescreener.repository.CandidateResumeRepository;
import com.example.resumescreener.repository.UserCredentialsRepository;
import com.example.resumescreener.repository.VacancyPostRepository;
import com.example.resumescreener.service.ResumeUploadService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ResumeApiController {

    private final ResumeUploadService resumeUploadService;
    private final CandidateResumeRepository candidateResumeRepository;
    private final VacancyPostRepository vacancyPostRepository;
    private final UserCredentialsRepository userCredentialsRepository;

    public ResumeApiController(ResumeUploadService resumeUploadService,
                             CandidateResumeRepository candidateResumeRepository,
                             VacancyPostRepository vacancyPostRepository,
                             UserCredentialsRepository userCredentialsRepository) {
        this.resumeUploadService = resumeUploadService;
        this.candidateResumeRepository = candidateResumeRepository;
        this.vacancyPostRepository = vacancyPostRepository;
        this.userCredentialsRepository = userCredentialsRepository;
    }

    @PostMapping(value = "/resumes/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    public Map<String, Object> upload(@RequestParam(value = "files", required = false) List<MultipartFile> files,
                                    Authentication authentication) {
        if (files == null || files.isEmpty()) {
            return Map.of(
                "stored", 0,
                "failed", 0,
                "fastApiNotified", false,
                "savedFiles", List.of(),
                "uploaded", List.of(),
                "message", "No PDF files were provided."
            );
        }

        UserCredentials user = userCredentialsRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return Map.of(
                "stored", 0,
                "failed", files.size(),
                "fastApiNotified", false,
                "savedFiles", List.of(),
                "uploaded", List.of(),
                "message", "User account could not be found."
            );
        }

        return resumeUploadService.processBatch(files, user.getId());
    }

    @GetMapping("/candidates")
    public List<CandidateResume> candidates(Authentication authentication) {
        if (authentication != null && authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
            return candidateResumeRepository.findAllByOrderByIdDesc();
        }

        UserCredentials user = userCredentialsRepository.findByUsername(authentication.getName()).orElse(null);
        if (user == null) {
            return List.of();
        }
        return candidateResumeRepository.findByUserIdOrderByIdDesc(user.getId());
    }

    @GetMapping("/vacancies")
    public List<VacancyPost> vacancies() {
        return vacancyPostRepository.findAllByOrderByIdDesc();
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "ok", "service", "resume-screener-java");
    }
}
