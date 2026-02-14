package com.TalentForge.talentforge.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ResumeStorageService {

    private final Path resumeDir;

    public ResumeStorageService(@Value("${app.storage.resume-dir}") String resumeDir) throws IOException {
        this.resumeDir = Paths.get(resumeDir);
        Files.createDirectories(this.resumeDir);
    }

    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String original = file.getOriginalFilename() == null ? "resume.bin" : file.getOriginalFilename();
        String safeName = UUID.randomUUID() + "_" + original.replaceAll("[^a-zA-Z0-9._-]", "_");
        Path target = resumeDir.resolve(safeName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toString();
    }
}
