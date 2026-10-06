package com.example.resumescreener.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidates",
       indexes = @Index(name = "idx_candidates_uploaded_by", columnList = "uploaded_by_id"))
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String fullName;
    private String email;
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String qualifications;

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Column(columnDefinition = "TEXT")
    private String experience;

    // Full text of the PDF, kept so the AI model can read it
    @Column(columnDefinition = "TEXT")
    private String rawText;

    // Who uploaded this resume. The id is used for access control (ids are never reused).
    // The username is only a copy for showing in the admin view.
    @Column(name = "uploaded_by_id")
    private Long uploadedById;

    @Column(name = "uploaded_by_username", length = 50)
    private String uploadedByUsername;

    private LocalDateTime uploadedAt = LocalDateTime.now();

    protected Candidate() {}

    public Candidate(String fileName, String fullName, String email, String phone,
                     String qualifications, String skills, String experience, String rawText,
                     Long uploadedById, String uploadedByUsername) {
        this.fileName = fileName;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.qualifications = qualifications;
        this.skills = skills;
        this.experience = experience;
        this.rawText = rawText;
        this.uploadedById = uploadedById;
        this.uploadedByUsername = uploadedByUsername;
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
    public Long getUploadedById() { return uploadedById; }
    public String getUploadedByUsername() { return uploadedByUsername; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}