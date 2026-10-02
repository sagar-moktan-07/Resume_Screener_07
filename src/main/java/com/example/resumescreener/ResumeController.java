package com.example.resumescreener;

import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ResumeController {

    private final ResumeService service;
    private final CandidateRepository repository;

    public ResumeController(ResumeService service, CandidateRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @PostMapping(value = "/resumes/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResult upload(@RequestParam("files") List<MultipartFile> files) {
        return service.processBatch(files);
    }

    @GetMapping("/candidates")
    public List<Candidate> candidates() {
        return repository.findAll(Sort.by(Sort.Direction.DESC, "id"));
    }
}