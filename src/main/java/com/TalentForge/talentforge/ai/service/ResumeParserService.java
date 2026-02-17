package com.TalentForge.talentforge.ai.service;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class ResumeParserService {

    private static final int MAX_TIKA_CHARS = 20000;

    private final Tika tika = new Tika();

    public String extractText(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return "";
        }

        byte[] bytes = file.getBytes();
        String directDecode = decodeTextFallback(file, bytes);
        if (!directDecode.isBlank()) {
            return directDecode;
        }

        try {
            String parsed = truncate(sanitize(tika.parseToString(new ByteArrayInputStream(bytes))), MAX_TIKA_CHARS);
            if (!parsed.isBlank()) {
                return parsed;
            }

            return decodeTextFallback(file, bytes);
        } catch (Exception ex) {
            try {
                return decodeTextFallback(file, bytes);
            } catch (Exception fallbackEx) {
                throw new IOException("Failed to parse resume file", ex);
            }
        }
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace('\u0000', ' ')
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll(" {2,}", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private boolean looksTextLike(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null) {
            String normalized = contentType.toLowerCase(Locale.ROOT);
            if (normalized.startsWith("text/")) {
                return true;
            }
            if (normalized.contains("json") || normalized.contains("xml")) {
                return true;
            }
        }

        String name = file.getOriginalFilename();
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".txt") || lower.endsWith(".md") || lower.endsWith(".csv") || lower.endsWith(".rtf");
    }

    private String decodeTextFallback(MultipartFile file, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        if (!looksTextLike(file)) {
            return "";
        }

        String utf8 = sanitize(new String(bytes, StandardCharsets.UTF_8));
        if (!utf8.isBlank()) {
            return utf8;
        }

        String latin1 = sanitize(new String(bytes, StandardCharsets.ISO_8859_1));
        if (!latin1.isBlank()) {
            return latin1;
        }

        return "";
    }

    private String truncate(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxChars);
    }
}
