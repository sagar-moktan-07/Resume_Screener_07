package com.example.resumescreener.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "finalized_candidate")
public class FinalizedCandidate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private CandidateResume candidate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screening_run_id", nullable = false)
    private ScreeningRun screeningRun;

    @Column(nullable = false)
    private Double finalScore = 0.0;

    @Column(columnDefinition = "TEXT")
    private String recommendation;

    @Column(columnDefinition = "TEXT")
    private String qwenEvaluation;

    @Column(nullable = false)
    private Integer rank = 0;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public FinalizedCandidate() {
    }

    public Long getId() {
        return id;
    }

    public CandidateResume getCandidate() {
        return candidate;
    }

    public void setCandidate(CandidateResume candidate) {
        this.candidate = candidate;
    }

    public ScreeningRun getScreeningRun() {
        return screeningRun;
    }

    public void setScreeningRun(ScreeningRun screeningRun) {
        this.screeningRun = screeningRun;
    }

    public Double getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(Double finalScore) {
        this.finalScore = finalScore;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public String getQwenEvaluation() {
        return qwenEvaluation;
    }

    public void setQwenEvaluation(String qwenEvaluation) {
        this.qwenEvaluation = qwenEvaluation;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
