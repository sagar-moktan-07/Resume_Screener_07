package com.example.resumescreener.service;

import com.example.resumescreener.entity.CandidateResume;
import com.example.resumescreener.repository.CandidateResumeRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ResumeUploadService {
    private final CandidateResumeRepository candidateResumeRepository;
    private final PdfTextExtractor pdfTextExtractor;
    private final FastApiClient fastApiClient;

    public ResumeUploadService(CandidateResumeRepository candidateResumeRepository,
                              PdfTextExtractor pdfTextExtractor,
                              FastApiClient fastApiClient) {
        this.candidateResumeRepository = candidateResumeRepository;
        this.pdfTextExtractor = pdfTextExtractor;
        this.fastApiClient = fastApiClient;
    }

    public List<CandidateResume> upload(List<MultipartFile> files) {
        List<CandidateResume> saved = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;
            try {
                String rawText = pdfTextExtractor.extractText(file);
                CandidateResume resume = new CandidateResume(
                        file.getOriginalFilename(),
                        extractName(rawText),
                        extractEmail(rawText),
                        extractPhone(rawText),
                        extractQualification(rawText),
                        extractSkills(rawText),
                        extractExperience(rawText),
                        rawText
                );
                saved.add(candidateResumeRepository.save(resume));
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not read file: " + file.getOriginalFilename(), e);
            }
        }

        if (!saved.isEmpty()) {
            fastApiClient.notifyBatch(saved.stream().map(CandidateResume::getId).toList());
        }
        return saved;
    }

    private String extractName(String text) {
        if (text == null || text.isBlank()) return "Unknown candidate";
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String candidate = line.trim();
            if (candidate.length() > 2 && candidate.length() < 80 && !candidate.contains("@") && !candidate.matches(".*\\d.*")) {
                return candidate;
            }
        }
        return "Unknown candidate";
    }

    private String extractEmail(String text) {
        if (text == null || text.isBlank()) return null;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}").matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private String extractPhone(String text) {
        if (text == null || text.isBlank()) return null;
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?:\\+?\\d[\\d\\s().-]{7,}\\d)").matcher(text);
        return matcher.find() ? matcher.group().replace(" ", "") : null;
    }

    private String extractQualification(String text) {
        return text == null ? "" : text.substring(0, Math.min(text.length(), 1500));
    }

    private String extractSkills(String text) {
        return text == null ? "" : text;
    }

    private String extractExperience(String text) {
        return text == null ? "" : text;
    }
}
