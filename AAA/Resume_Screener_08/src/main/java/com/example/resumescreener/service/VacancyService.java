package com.example.resumescreener.service;

import com.example.resumescreener.entity.VacancyPost;
import com.example.resumescreener.repository.VacancyPostRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VacancyService {
    private final VacancyPostRepository repository;

    public VacancyService(VacancyPostRepository repository) {
        this.repository = repository;
    }

    public VacancyPost create(VacancyPost vacancy) {
        vacancy.setCreatedAt(LocalDateTime.now());
        vacancy.setUpdatedAt(LocalDateTime.now());
        return repository.save(vacancy);
    }

    public List<VacancyPost> findAll() {
        return repository.findAll();
    }

    public VacancyPost findById(Long id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Vacancy not found: " + id));
    }
}
