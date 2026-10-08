package com.example.resumescreener.controller;

import com.example.resumescreener.entity.CandidateResume;
import com.example.resumescreener.entity.VacancyPost;
import com.example.resumescreener.repository.CandidateResumeRepository;
import com.example.resumescreener.repository.VacancyPostRepository;
import com.example.resumescreener.service.ResumeUploadService;
import com.example.resumescreener.service.ScreeningService;
import com.example.resumescreener.service.VacancyService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
public class DashboardController {
    private final VacancyService vacancyService;
    private final CandidateResumeRepository candidateResumeRepository;
    private final ScreeningService screeningService;
    private final ResumeUploadService resumeUploadService;

    public DashboardController(VacancyService vacancyService,
                              CandidateResumeRepository candidateResumeRepository,
                              ScreeningService screeningService,
                              ResumeUploadService resumeUploadService) {
        this.vacancyService = vacancyService;
        this.candidateResumeRepository = candidateResumeRepository;
        this.screeningService = screeningService;
        this.resumeUploadService = resumeUploadService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("vacancies", vacancyService.findAll());
        model.addAttribute("candidates", candidateResumeRepository.findAll());
        model.addAttribute("newVacancy", new VacancyPost());
        return "dashboard";
    }

    @PostMapping("/vacancies")
    public String createVacancy(@ModelAttribute("newVacancy") VacancyPost vacancy) {
        vacancyService.create(vacancy);
        return "redirect:/dashboard";
    }

    @PostMapping("/upload")
    public String upload(@RequestParam("files") List<MultipartFile> files) {
        resumeUploadService.upload(files);
        return "redirect:/dashboard";
    }

    @PostMapping("/screen")
    public String screen(@RequestParam("vacancyId") Long vacancyId) {
        screeningService.trigger(vacancyId);
        return "redirect:/dashboard";
    }
}
