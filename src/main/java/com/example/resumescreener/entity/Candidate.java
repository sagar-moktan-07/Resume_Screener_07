package com.example.resumescreener.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(length = 255)
    private String email;

    @Column(length = 255)
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String qualifications;

    @Column(columnDefinition = "TEXT")
    private String skills;

    @Column(columnDefinition = "TEXT")
    private String experience;

    @Column(name = "raw_text", columnDefinition = "TEXT")
    private String rawText;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    /** DB default is 'upload'; Hibernate sends this so NOT NULL is always satisfied. */
    @Column(nullable = false, length = 20)
    private String source = "upload";

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
        this.source = "upload";
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
    public String getSource() { return source; }
}
