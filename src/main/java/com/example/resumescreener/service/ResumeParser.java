package com.example.resumescreener.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResumeParser {

    public record ParsedResume(String fullName, String email, String phone,
                               String qualifications, String skills, String experience) {}

    private static final Pattern EMAIL =
            Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE =
            Pattern.compile("\\+?\\d[\\d\\s().-]{7,}\\d");

    // Heading words that start each section we care about
    private static final Map<String, Set<String>> HEADINGS = Map.of(
            "qualifications", Set.of("education", "qualification", "qualifications",
                    "academic background", "academics", "certifications"),
            "skills", Set.of("skills", "technical skills", "key skills",
                    "core competencies", "skill set", "skillset"),
            "experience", Set.of("experience", "work experience", "professional experience",
                    "employment history", "work history")
    );

    // Other headings: they end the current section so content doesn't leak into it
    private static final Set<String> OTHER_HEADINGS = Set.of(
            "summary", "objective", "profile", "projects", "references",
            "languages", "interests", "hobbies", "awards", "achievements", "declaration");

    public ParsedResume parse(String text, String fileName) {
        String[] lines = text.split("\\R");

        Map<String, StringBuilder> sections = new HashMap<>();
        sections.put("qualifications", new StringBuilder());
        sections.put("skills", new StringBuilder());
        sections.put("experience", new StringBuilder());

        String current = null;
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;

            String heading = detectHeading(line);
            if (heading != null) {
                current = heading;
                continue;
            }
            if (current != null && sections.containsKey(current)) {
                sections.get(current).append(line).append("\n");
            }
        }

        return new ParsedResume(
                findName(lines, fileName),
                findEmail(text),
                findPhone(text),
                clean(sections.get("qualifications")),
                clean(sections.get("skills")),
                clean(sections.get("experience")));
    }

    private String detectHeading(String line) {
        if (line.length() > 40) return null;
        String key = line.toLowerCase().replaceAll("[^a-z ]", "").trim();

        for (var entry : HEADINGS.entrySet()) {
            if (entry.getValue().contains(key)) return entry.getKey();
        }
        return OTHER_HEADINGS.contains(key) ? "other" : null;
    }

    private String findName(String[] lines, String fileName) {
        int checked = 0;
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            if (++checked > 5) break;

            String lower = line.toLowerCase();
            if (lower.equals("resume") || lower.equals("curriculum vitae") || lower.equals("cv")) continue;

            if (line.length() <= 50 && !line.contains("@") && !line.matches(".*\\d.*")) {
                return line;
            }
        }
        return fileName.replaceFirst("(?i)\\.pdf$", "");
    }

    private String findEmail(String text) {
        Matcher m = EMAIL.matcher(text);
        return m.find() ? m.group() : null;
    }

    private String findPhone(String text) {
        // Only look at the top of the resume to avoid matching date ranges
        String header = text.substring(0, Math.min(text.length(), 1500));
        Matcher m = PHONE.matcher(header);
        while (m.find()) {
            String found = m.group().trim();
            if (found.replaceAll("\\D", "").length() >= 9) return found;
        }
        return null;
    }

    private String clean(StringBuilder sb) {
        String s = sb.toString().trim();
        return s.isEmpty() ? null : s;
    }
}
