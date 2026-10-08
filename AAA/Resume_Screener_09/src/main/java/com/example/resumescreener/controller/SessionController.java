package com.example.resumescreener.controller;

import com.example.resumescreener.entity.UserCredentials;
import com.example.resumescreener.entity.VacancyPost;
import com.example.resumescreener.entity.FinalizedCandidate;
import com.example.resumescreener.repository.CandidateResumeRepository;
import com.example.resumescreener.repository.FinalizedCandidateRepository;
import com.example.resumescreener.repository.UserCredentialsRepository;
import com.example.resumescreener.repository.VacancyPostRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Locale;
import java.util.regex.Pattern;

@Controller
public class SessionController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final VacancyPostRepository vacancyPostRepository;
    private final CandidateResumeRepository candidateResumeRepository;
    private final UserCredentialsRepository userCredentialsRepository;
    private final FinalizedCandidateRepository finalizedCandidateRepository;
    private final PasswordEncoder passwordEncoder;

    public SessionController(VacancyPostRepository vacancyPostRepository,
                            CandidateResumeRepository candidateResumeRepository,
                            UserCredentialsRepository userCredentialsRepository,
                            FinalizedCandidateRepository finalizedCandidateRepository,
                            PasswordEncoder passwordEncoder) {
        this.vacancyPostRepository = vacancyPostRepository;
        this.candidateResumeRepository = candidateResumeRepository;
        this.userCredentialsRepository = userCredentialsRepository;
        this.finalizedCandidateRepository = finalizedCandidateRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping({"/", "/login"})
    public String login(@RequestParam(value = "error", required = false) String error,
                        Model model) {
        if (error != null) {
            model.addAttribute("loginError", "Invalid username or password.");
        }
        return "login";
    }

    @GetMapping("/signup")
    public String signup(Model model) {
        model.addAttribute("user", new UserCredentials());
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("user") UserCredentials userCredentials,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "signup";
        }

        String name = userCredentials.getName() == null ? "" : userCredentials.getName().trim();
        String username = userCredentials.getUsername() == null ? "" : userCredentials.getUsername().trim();
        String email = userCredentials.getEmail() == null ? "" : userCredentials.getEmail().trim();
        String password = userCredentials.getPassword() == null ? "" : userCredentials.getPassword();

        if (name.isEmpty() || username.isEmpty() || email.isEmpty() || password.isBlank()) {
            redirectAttributes.addFlashAttribute("signupError", "Name, username, email, and password are required.");
            return "redirect:/signup";
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            redirectAttributes.addFlashAttribute("signupError", "Please enter a valid email address.");
            return "redirect:/signup";
        }

        String normalizedUsername = username.trim();
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (userCredentialsRepository.existsByUsername(normalizedUsername)) {
            redirectAttributes.addFlashAttribute("signupError", "This username already exists. Please choose another one.");
            return "redirect:/signup";
        }

        if (userCredentialsRepository.existsByEmail(normalizedEmail)) {
            redirectAttributes.addFlashAttribute("signupError", "This email address is already registered.");
            return "redirect:/signup";
        }

        userCredentials.setName(name);
        userCredentials.setUsername(normalizedUsername);
        userCredentials.setEmail(normalizedEmail);
        userCredentials.setPassword(passwordEncoder.encode(password));
        userCredentialsRepository.save(userCredentials);

        redirectAttributes.addFlashAttribute("signupSuccess", "Account created successfully. Please sign in.");
        return "redirect:/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            return "redirect:/normal-user-dashboard";
        }

        model.addAttribute("isAdmin", true);
        model.addAttribute("vacancies", vacancyPostRepository.findAllByOrderByIdDesc());
        model.addAttribute("candidates", candidateResumeRepository.findAllByOrderByIdDesc());
        model.addAttribute("vacancy", new VacancyPost());
        return "dashboard";
    }

    @GetMapping("/normal-user-dashboard")
    @PreAuthorize("hasRole('USER')")
    public String normalUserDashboard(Model model, Authentication authentication) {
        UserCredentials user = userCredentialsRepository.findByUsername(authentication.getName()).orElse(null);
        model.addAttribute("user", user);
        model.addAttribute("candidate", user == null ? null : candidateResumeRepository.findFirstByUserIdOrderByIdDesc(user.getId()).orElse(null));
        model.addAttribute("hasUploadedResume", user != null && candidateResumeRepository.existsByUserId(user.getId()));
        return "normal-user-dashboard";
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminUsers(Model model) {
        model.addAttribute("users", userCredentialsRepository.findAllByOrderByIdDesc());
        return "admin-users";
    }

    @GetMapping("/admin/finalized")
    @PreAuthorize("hasRole('ADMIN')")
    public String finalizedCandidates(Model model) {
        var finalized = finalizedCandidateRepository.findAllByOrderByIdDesc();
        var vacancyMap = new java.util.HashMap<Long, String>();
        var candidateNameMap = new java.util.HashMap<Long, String>();
        var candidateEmailMap = new java.util.HashMap<Long, String>();

        for (var vacancy : vacancyPostRepository.findAllByOrderByIdDesc()) {
            vacancyMap.put(vacancy.getId(), vacancy.getJobTitle());
        }
        for (var candidate : candidateResumeRepository.findAllByOrderByIdDesc()) {
            candidateNameMap.put(candidate.getId(), candidate.getFullName());
            candidateEmailMap.put(candidate.getId(), candidate.getEmail());
        }

        model.addAttribute("finalizedCandidates", finalized);
        model.addAttribute("vacancyMap", vacancyMap);
        model.addAttribute("candidateNameMap", candidateNameMap);
        model.addAttribute("candidateEmailMap", candidateEmailMap);
        return "finalized-candidates";
    }

    @PostMapping("/admin/finalized/{id}/decision")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateDecision(@PathVariable Long id,
                                @RequestParam String decision,
                                RedirectAttributes redirectAttributes) {
        FinalizedCandidate candidate = finalizedCandidateRepository.findById(id).orElse(null);
        if (candidate != null) {
            String normalized = decision == null ? "" : decision.trim().toLowerCase(Locale.ROOT);
            if (normalized.equals("approved") || normalized.equals("rejected") || normalized.equals("backup") || normalized.equals("recommended") || normalized.equals("shortlist")) {
                candidate.setDecision(normalized);
                finalizedCandidateRepository.save(candidate);
                redirectAttributes.addFlashAttribute("message", "Decision updated to '" + normalized + "'.");
            }
        }
        return "redirect:/admin/finalized";
    }

    @PostMapping("/vacancies")
    public String createVacancy(@ModelAttribute("vacancy") VacancyPost vacancyPost) {
        vacancyPost.setUpdatedAt(java.time.LocalDateTime.now());
        vacancyPostRepository.save(vacancyPost);
        return "redirect:/dashboard";
    }

    @PostMapping("/admin/users/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (userCredentialsRepository.existsById(id)) {
            userCredentialsRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "User removed successfully.");
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/admin/candidates/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteCandidate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (candidateResumeRepository.existsById(id)) {
            candidateResumeRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "Candidate removed successfully.");
        }
        return "redirect:/dashboard";
    }
}
