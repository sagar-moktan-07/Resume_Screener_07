package com.example.resumescreener.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "vacancy_embedding")
public class VacancyEmbedding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vacancy_id", nullable = false, unique = true)
    private VacancyPost vacancy;

    @Column(columnDefinition = "TEXT")
    private String embeddingText;

    @Column(columnDefinition = "vector(768)")
    private String embedding;

    public VacancyEmbedding() {
    }

    public Long getId() {
        return id;
    }

    public VacancyPost getVacancy() {
        return vacancy;
    }

    public void setVacancy(VacancyPost vacancy) {
        this.vacancy = vacancy;
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
