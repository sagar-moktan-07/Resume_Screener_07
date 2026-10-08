package com.example.resumescreener.controller;

import com.example.resumescreener.entity.CandidateResume;
import com.example.resumescreener.entity.VacancyPost;
import com.example.resumescreener.repository.CandidateResumeRepository;
import com.example.resumescreener.repository.VacancyPostRepository;
import com.example.resumescreener.service.ResumeUploadService;
import com.example.resumescreener.service.ScreeningService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final VacancyPostRepository vacancyPostRepository;
    private final CandidateResumeRepository candidateResumeRepository;
    private final ResumeUploadService resumeUploadService;
    private final ScreeningService screeningService;

    public ApiController(VacancyPostRepository vacancyPostRepository,
                        CandidateResumeRepository candidateResumeRepository,
                        ResumeUploadService resumeUploadService,
                        ScreeningService screeningService) {
        this.vacancyPostRepository = vacancyPostRepository;
        this.candidateResumeRepository = candidateResumeRepository;
        this.resumeUploadService = resumeUploadService;
        this.screeningService = screeningService;
    }

    @GetMapping("/vacancies")
    public List<VacancyPost> vacancies() {
        return vacancyPostRepository.findAll();
    }

    @PostMapping("/vacancies")
    public VacancyPost createVacancy(@RequestBody VacancyPost vacancy) {
        return vacancyPostRepository.save(vacancy);
    }

    @GetMapping("/candidates")
    public List<CandidateResume> candidates() {
        return candidateResumeRepository.findAll();
    }

    @PostMapping("/candidates/upload")
    public List<CandidateResume> upload(@RequestParam("files") List<MultipartFile> files) {
        return resumeUploadService.upload(files);
    }

    @PostMapping("/screen/{vacancyId}")
    public ResponseEntity<String> screen(@PathVariable Long vacancyId) {
        screeningService.trigger(vacancyId);
        return ResponseEntity.ok("Screening started for vacancy " + vacancyId);
    }
}
