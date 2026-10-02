package com.example.resumescreener;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String fullName;
    private String email;
    private String phone;

    @Column(length = 10000)
    private String qualifications;

    @Column(length = 10000)
    private String skills;

    @Column(length = 10000)
    private String experience;

    // Full text of the PDF, kept so the AI model can re-read it later
    @Column(length = 100000)
    private String rawText;

    private LocalDateTime uploadedAt = LocalDateTime.now();

    protected Candidate() {}

    public Candidate(String fileName, String fullName, String email, String phone,
                     String qualifications, String skills, String experience, String rawText) {
        this.fileName = fileName;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.qualifications = qualifications;
        this.skills = skills;
        this.experience = experience;
        this.rawText = rawText;
    }

    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getQualifications() { return qualifications; }
    public String getSkills() { return skills; }
    public String getExperience() { return experience; }
    public String getRawText() { return rawText; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}