package com.example.resumescreener.service;

import com.example.resumescreener.client.FastApiNotifier;
import com.example.resumescreener.dto.UploadResult;
import com.example.resumescreener.dto.UploadResult.FileResult;
import com.example.resumescreener.entity.Candidate;
import com.example.resumescreener.repository.CandidateRepository;
import com.example.resumescreener.service.ResumeParser.ParsedResume;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ResumeService {

    private final PdfTextExtractor extractor;
    private final ResumeParser parser;
    private final CandidateRepository repository;
    private final FastApiNotifier notifier;

    public ResumeService(PdfTextExtractor extractor, ResumeParser parser,
                         CandidateRepository repository, FastApiNotifier notifier) {
        this.extractor = extractor;
        this.parser = parser;
        this.repository = repository;
        this.notifier = notifier;
    }

    // Not @Transactional on purpose: each save() commits on its own,
    // so by the time we notify FastAPI every row is already in the database.
    public UploadResult processBatch(List<MultipartFile> files, Long ownerId, String ownerUsername) {
        String batchId = UUID.randomUUID().toString();
        List<FileResult> results = new ArrayList<>();
        List<Long> savedIds = new ArrayList<>();

        for (MultipartFile file : files) {
            String name = file.getOriginalFilename() == null ? "unknown.pdf" : file.getOriginalFilename();

            try {
                if (file.isEmpty() || !name.toLowerCase().endsWith(".pdf")) {
                    results.add(new FileResult(name, "SKIPPED", "Not a PDF file", null));
                    continue;
                }

                String text = extractor.extract(file);
                if (text.isBlank()) {
                    results.add(new FileResult(name, "FAILED", "No text found (scanned PDF?)", null));
                    continue;
                }

                ParsedResume p = parser.parse(text, name);

                Candidate saved = repository.save(new Candidate(
                        name,
                        cut(p.fullName(), 255),
                        cut(p.email(), 255),
                        cut(p.phone(), 255),
                        cut(p.qualifications(), 10000),
                        cut(p.skills(), 10000),
                        cut(p.experience(), 10000),
                        cut(text, 100000),
                        ownerId,
                        ownerUsername));

                savedIds.add(saved.getId());
                results.add(new FileResult(name, "STORED", "OK", saved.getId()));

            } catch (Exception e) {
                // One bad PDF should not stop the rest of the batch
                results.add(new FileResult(name, "FAILED", e.getMessage(), null));
            }
        }

        // All saves are committed, now tell FastAPI
        boolean notified = !savedIds.isEmpty() && notifier.notifyDataReady(batchId, savedIds);

        int failed = results.size() - savedIds.size();
        return new UploadResult(batchId, savedIds.size(), failed, notified, results);
    }

    private String cut(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}