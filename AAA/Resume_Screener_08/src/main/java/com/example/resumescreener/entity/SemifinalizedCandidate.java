package com.example.resumescreener.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "semifinalized_candidate")
public class SemifinalizedCandidate {
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
    private Double score = 0.0;

    @Column(nullable = false)
    private Integer rank = 0;

    public SemifinalizedCandidate() {
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

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }
}
