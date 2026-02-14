package com.TalentForge.talentforge.ai.service;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class ResumeParserService {

    private final Tika tika = new Tika();

    public String extractText(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return "";
        }

        try {
            return tika.parseToString(file.getInputStream());
        } catch (Exception ex) {
            throw new IOException("Failed to parse resume file", ex);
        }
    }
}
