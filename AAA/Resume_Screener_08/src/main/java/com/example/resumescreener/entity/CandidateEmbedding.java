package com.example.resumescreener.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "candidate_embedding")
public class CandidateEmbedding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id", nullable = false, unique = true)
    private CandidateResume candidate;

    @Column(columnDefinition = "TEXT")
    private String embeddingText;

    @Column(columnDefinition = "vector(768)")
    private String embedding;

    public CandidateEmbedding() {
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

    public String getEmbeddingText() {
        return embeddingText;
    }

    public void setEmbeddingText(String embeddingText) {
        this.embeddingText = embeddingText;
    }

    public String getEmbedding() {
        return embedding;
    }

    public void setEmbedding(String embedding) {
        this.embedding = embedding;
    }
}
