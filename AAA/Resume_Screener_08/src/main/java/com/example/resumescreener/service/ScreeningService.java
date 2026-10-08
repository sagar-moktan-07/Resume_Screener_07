package com.example.resumescreener.service;

import com.example.resumescreener.entity.CandidateResume;
import com.example.resumescreener.entity.ScreeningResult;
import com.example.resumescreener.entity.ScreeningRun;
import com.example.resumescreener.entity.VacancyPost;
import com.example.resumescreener.repository.CandidateResumeRepository;
import com.example.resumescreener.repository.ScreeningResultRepository;
import com.example.resumescreener.repository.ScreeningRunRepository;
import com.example.resumescreener.repository.VacancyPostRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ScreeningService {
    private final CandidateResumeRepository candidateResumeRepository;
    private final VacancyPostRepository vacancyPostRepository;
    private final ScreeningRunRepository screeningRunRepository;
    private final ScreeningResultRepository screeningResultRepository;

    public ScreeningService(CandidateResumeRepository candidateResumeRepository,
                           VacancyPostRepository vacancyPostRepository,
                           ScreeningRunRepository screeningRunRepository,
                           ScreeningResultRepository screeningResultRepository) {
        this.candidateResumeRepository = candidateResumeRepository;
        this.vacancyPostRepository = vacancyPostRepository;
        this.screeningRunRepository = screeningRunRepository;
        this.screeningResultRepository = screeningResultRepository;
    }

    public ScreeningRun trigger(Long vacancyId) {
        VacancyPost vacancy = vacancyPostRepository.findById(vacancyId)
                .orElseThrow(() -> new IllegalArgumentException("Vacancy not found: " + vacancyId));

        ScreeningRun run = new ScreeningRun();
        run.setVacancy(vacancy);
        run.setStatus("RUNNING");
        ScreeningRun saved = screeningRunRepository.save(run);

        List<CandidateResume> candidates = candidateResumeRepository.findAll();
        List<ScreeningResult> results = new ArrayList<>();
        for (CandidateResume candidate : candidates) {
            ScreeningResult result = new ScreeningResult();
            result.setScreeningRun(saved);
            result.setCandidate(candidate);
            result.setMcdmScore(simulateScore(candidate, vacancy));
            result.setRerankerScore(simulateScore(candidate, vacancy) + 0.1);
            result.setFinalScore(result.getRerankerScore());
            result.setReasoning("Candidate scored against vacancy requirements.");
            results.add(result);
        }

        results.sort(Comparator.comparing(ScreeningResult::getFinalScore).reversed());
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRank(i + 1);
        }
        screeningResultRepository.saveAll(results);

        saved.setStatus("DONE");
        saved.setFinishedAt(LocalDateTime.now());
        saved.setNotes("Processed " + results.size() + " candidate resumes.");
        return screeningRunRepository.save(saved);
    }

    private double simulateScore(CandidateResume candidate, VacancyPost vacancy) {
        double score = 0.0;
        String combined = (candidate.getSkills() == null ? "" : candidate.getSkills()) + " " +
                (candidate.getQualifications() == null ? "" : candidate.getQualifications()) + " " +
                (vacancy.getRequiredSkills() == null ? "" : vacancy.getRequiredSkills());

        int matches = 0;
        for (String word : List.of("java", "spring", "postgres", "sql", "api", "python", "docker", "cloud", "rest")) {
            if (combined.toLowerCase().contains(word)) matches++;
        }
        score = (double) matches / 9.0;
        return Math.min(1.0, score + 0.15);
    }
}
