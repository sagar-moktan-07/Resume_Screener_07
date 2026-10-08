package com.example.resumescreener.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "screening_result")
public class ScreeningResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "screening_run_id", nullable = false)
    private ScreeningRun screeningRun;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false)
    private CandidateResume candidate;

    @Column(nullable = false)
    private Double mcdmScore = 0.0;

    @Column(nullable = false)
    private Double rerankerScore = 0.0;

    @Column(nullable = false)
    private Double finalScore = 0.0;

    @Column(columnDefinition = "TEXT")
    private String reasoning;

    @Column(nullable = false)
    private Integer rank = 0;

    public ScreeningResult() {
    }

    public Long getId() {
        return id;
    }

    public ScreeningRun getScreeningRun() {
        return screeningRun;
    }

    public void setScreeningRun(ScreeningRun screeningRun) {
        this.screeningRun = screeningRun;
    }

    public CandidateResume getCandidate() {
        return candidate;
    }

    public void setCandidate(CandidateResume candidate) {
        this.candidate = candidate;
    }

    public Double getMcdmScore() {
        return mcdmScore;
    }

    public void setMcdmScore(Double mcdmScore) {
        this.mcdmScore = mcdmScore;
    }

    public Double getRerankerScore() {
        return rerankerScore;
    }

    public void setRerankerScore(Double rerankerScore) {
        this.rerankerScore = rerankerScore;
    }

    public Double getFinalScore() {
        return finalScore;
    }

    public void setFinalScore(Double finalScore) {
        this.finalScore = finalScore;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }
}
