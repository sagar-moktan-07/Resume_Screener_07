package com.example.resumescreener.controller;

import com.example.resumescreener.dto.CandidateView;
import com.example.resumescreener.dto.UploadResult;
import com.example.resumescreener.entity.Candidate;
import com.example.resumescreener.entity.Role;
import com.example.resumescreener.entity.UserAccount;
import com.example.resumescreener.repository.CandidateRepository;
import com.example.resumescreener.repository.UserRepository;
import com.example.resumescreener.service.ResumeService;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ResumeController {

    private final ResumeService service;
    private final CandidateRepository repository;
    private final UserRepository users;

    public ResumeController(ResumeService service, CandidateRepository repository,
                            UserRepository users) {
        this.service = service;
        this.repository = repository;
        this.users = users;
    }

    @PostMapping(value = "/resumes/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResult upload(@RequestParam("files") List<MultipartFile> files, Authentication auth) {
        UserAccount me = currentUser(auth);
        // The owner comes from the logged-in session, never from the request
        return service.processBatch(files, me.getId(), me.getUsername());
    }

    @GetMapping("/candidates")
    public List<CandidateView> candidates(Authentication auth) {
        UserAccount me = currentUser(auth);

        List<Candidate> rows = (me.getRole() == Role.ADMIN)
                ? repository.findAll(Sort.by(Sort.Direction.DESC, "id"))      // admin: everything
                : repository.findByUploadedByIdOrderByIdDesc(me.getId());     // user: only own uploads

        return rows.stream().map(CandidateView::from).toList();
    }

    // ADMIN only (enforced in SecurityConfig)
    @DeleteMapping("/candidates/{id}")
    public ResponseEntity<Void> deleteCandidate(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private UserAccount currentUser(Authentication auth) {
        return users.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not logged in"));
    }
}