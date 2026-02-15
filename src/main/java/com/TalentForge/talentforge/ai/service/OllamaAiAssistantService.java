package com.TalentForge.talentforge.ai.service;

import com.TalentForge.talentforge.ai.dto.AiResumeScoreResult;
import com.TalentForge.talentforge.common.exception.AiServiceUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OllamaAiAssistantService implements AiAssistantService {

    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\\r\\n\\t]]");

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService aiExecutor = Executors.newCachedThreadPool();

    @Value("${app.ai.timeout-ms:30000}")
    private long aiTimeoutMs;

    @Value("${app.ai.max-retries:2}")
    private int aiMaxRetries;

    @Value("${spring.ai.ollama.chat.options.model:llama3.2:latest}")
    private String configuredModel;

    @Override
    public String checkJobBias(String title, String description, String requirements) {
        try {
            String prompt = """
                    You are Talentforge hiring fairness assistant.
                    Review the job post below and return:
                    1) Risk level (LOW, MEDIUM, HIGH)
                    2) Specific biased phrases
                    3) Inclusive rewrite suggestions

                    Title: %s
                    Description: %s
                    Requirements: %s
                    """.formatted(
                    sanitizeForPrompt(title, 1000),
                    sanitizeForPrompt(description, 5000),
                    sanitizeForPrompt(requirements, 3000)
            );

            return callAi(prompt);
        } catch (Exception ex) {
            return "Bias check unavailable. Please review language manually for age, gender, and cultural exclusions.";
        }
    }

    @Override
    public AiResumeScoreResult scoreResume(String jobText, String resumeText) {
        String safeJobText = sanitizeForPrompt(jobText, 7000);
        String safeResumeText = sanitizeForPrompt(resumeText, 9000);

        try {
            String prompt = """
                    You are Talentforge resume scoring assistant.
                    Score candidate fit from 0 to 100.

                    Scoring rubric:
                    - Skill alignment with requirements (40%%)
                    - Relevant experience evidence (30%%)
                    - Domain/context relevance (20%%)
                    - Communication clarity (10%%)

                    Return ONLY valid JSON in this exact shape:
                    {
                      "score": 0,
                      "skills": ["skill one", "skill two"],
                      "reasoning": "one concise paragraph",
                      "strengths": ["optional"],
                      "gaps": ["optional"]
                    }

                    JOB DESCRIPTION:
                    %s

                    CANDIDATE PROFILE:
                    %s
                    """.formatted(safeJobText, safeResumeText);

            String raw = callAi(prompt);
            JsonNode node = parseJsonResponse(raw);

            double score = Math.max(0, Math.min(100, node.path("score").asDouble(0)));
            String reason = normalizeReason(extractReason(node));
            String matchingKeywords = extractSkills(node);

            if (matchingKeywords.isBlank()) {
                matchingKeywords = extractKeywordFallback(safeJobText, safeResumeText);
            }

            return new AiResumeScoreResult(score, reason, matchingKeywords);
        } catch (Exception ex) {
            return fallbackScore(safeJobText, safeResumeText);
        }
    }

    @Override
    public String generateChatReply(String message) {
        String prompt = """
                You are Talentforge assistant for candidates and recruiters.
                Keep answers practical and concise.
                User message: %s
                """.formatted(sanitizeForPrompt(message, 3000));

        try {
            return callAi(prompt);
        } catch (Exception ex) {
            throw new AiServiceUnavailableException(
                    "AI service unavailable. Verify Ollama is running and the configured model is loaded.",
                    ex
            );
        }
    }

    private String callAi(String prompt) {
        int attempts = Math.max(1, aiMaxRetries);
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            Future<String> future = aiExecutor.submit(() -> chatClientBuilder.build()
                    .prompt()
                    .user(prompt)
                    .call()
                    .content());

            try {
                String response = future.get(Math.max(1000L, aiTimeoutMs), TimeUnit.MILLISECONDS);
                if (response != null && !response.isBlank()) {
                    return response.trim();
                }
                throw new RuntimeException("AI returned an empty response from model " + configuredModel);
            } catch (Exception ex) {
                future.cancel(true);
                lastFailure = new RuntimeException("AI call timed out or failed on attempt " + attempt, ex);
            }
        }

        throw lastFailure == null ? new RuntimeException("AI call failed") : lastFailure;
    }

    private AiResumeScoreResult fallbackScore(String jobText, String resumeText) {
        String normalizedJob = jobText == null ? "" : jobText.toLowerCase(Locale.ROOT);
        String normalizedResume = resumeText == null ? "" : resumeText.toLowerCase(Locale.ROOT);

        Map<String, Long> keywordWeights = Arrays.stream(normalizedJob.split("[^a-z0-9+#]+"))
                .map(String::trim)
                .filter(token -> token.length() >= 3)
                .filter(token -> !isStopWord(token))
                .limit(80)
                .collect(Collectors.groupingBy(token -> token, LinkedHashMap::new, Collectors.counting()));

        if (keywordWeights.isEmpty()) {
            return new AiResumeScoreResult(0, "Talentforge fallback scoring used, but no useful job keywords were found.", "");
        }

        double totalWeight = keywordWeights.values().stream().mapToDouble(Long::doubleValue).sum();
        double matchedWeight = 0;
        Map<String, Long> matchedTokens = new LinkedHashMap<>();

        for (Map.Entry<String, Long> entry : keywordWeights.entrySet()) {
            long occurrences = countOccurrences(normalizedResume, entry.getKey());
            if (occurrences > 0) {
                matchedWeight += entry.getValue();
                matchedTokens.put(entry.getKey(), occurrences);
            }
        }

        double score = (matchedWeight / Math.max(1, totalWeight)) * 100.0;
        List<String> topMatched = matchedTokens.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(20)
                .toList();

        String matching = String.join(", ", topMatched);
        String reason = "Talentforge deterministic fallback used. Matched " + topMatched.size() + " weighted job keywords.";

        return new AiResumeScoreResult(Math.round(Math.max(0, Math.min(100, score)) * 10.0) / 10.0, reason, matching);
    }

    private JsonNode parseJsonResponse(String rawResponse) throws IOException {
        String body = extractJsonBody(rawResponse);

        try {
            return objectMapper.readTree(body);
        } catch (Exception firstFailure) {
            String extracted = extractFirstJsonObject(body);
            if (extracted == null || extracted.isBlank()) {
                throw new IOException("No JSON payload found in AI response", firstFailure);
            }
            return objectMapper.readTree(extracted);
        }
    }

    private String extractReason(JsonNode node) {
        String reasoning = node.path("reasoning").asText("");
        if (!reasoning.isBlank()) {
            return reasoning;
        }

        String reason = node.path("reason").asText("");
        if (!reason.isBlank()) {
            return reason;
        }

        List<String> details = new ArrayList<>();
        if (node.path("strengths").isArray()) {
            List<String> strengths = new ArrayList<>();
            node.path("strengths").forEach(item -> {
                String text = item.asText("").trim();
                if (!text.isBlank()) {
                    strengths.add(text);
                }
            });
            if (!strengths.isEmpty()) {
                details.add("Strengths: " + String.join("; ", strengths));
            }
        }

        if (node.path("gaps").isArray()) {
            List<String> gaps = new ArrayList<>();
            node.path("gaps").forEach(item -> {
                String text = item.asText("").trim();
                if (!text.isBlank()) {
                    gaps.add(text);
                }
            });
            if (!gaps.isEmpty()) {
                details.add("Gaps: " + String.join("; ", gaps));
            }
        }

        if (!details.isEmpty()) {
            return String.join(" ", details);
        }

        return "Model returned a score without explanation.";
    }

    private String extractSkills(JsonNode node) {
        Set<String> skills = new LinkedHashSet<>();

        JsonNode skillsNode = node.path("skills");
        if (skillsNode.isArray()) {
            skillsNode.forEach(item -> addSkill(skills, item.asText("")));
        } else {
            addSplitSkills(skills, skillsNode.asText(""));
        }

        JsonNode matchingKeywordsNode = node.path("matchingKeywords");
        if (matchingKeywordsNode.isArray()) {
            matchingKeywordsNode.forEach(item -> addSkill(skills, item.asText("")));
        } else {
            addSplitSkills(skills, matchingKeywordsNode.asText(""));
        }

        return skills.stream().limit(20).collect(Collectors.joining(", "));
    }

    private String extractKeywordFallback(String jobText, String resumeText) {
        Set<String> jobTerms = Arrays.stream(jobText.toLowerCase(Locale.ROOT).split("[^a-z0-9+#]+"))
                .filter(token -> token.length() >= 3)
                .filter(token -> !isStopWord(token))
                .limit(60)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> matched = jobTerms.stream()
                .filter(term -> resumeText.toLowerCase(Locale.ROOT).contains(term))
                .limit(20)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        return String.join(", ", matched);
    }

    private void addSplitSkills(Set<String> skills, String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }

        for (String token : raw.split("[,|;/]")) {
            addSkill(skills, token);
        }
    }

    private void addSkill(Set<String> skills, String rawSkill) {
        if (rawSkill == null) {
            return;
        }

        String normalized = rawSkill.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() < 2) {
            return;
        }

        if (normalized.length() > 40) {
            normalized = normalized.substring(0, 40);
        }

        skills.add(normalized);
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Talentforge AI returned no explanation for this score.";
        }

        String cleaned = CONTROL_CHARS.matcher(reason).replaceAll("").trim();
        if (cleaned.length() > 900) {
            return cleaned.substring(0, 900);
        }
        return cleaned;
    }

    private String sanitizeForPrompt(String value, int maxChars) {
        if (value == null) {
            return "";
        }

        String cleaned = CONTROL_CHARS.matcher(value).replaceAll(" ").trim();
        if (cleaned.length() <= maxChars) {
            return cleaned;
        }

        return cleaned.substring(0, maxChars);
    }

    private String extractJsonBody(String rawResponse) {
        if (rawResponse == null) {
            return "{}";
        }

        String trimmed = rawResponse.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > -1 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private String extractFirstJsonObject(String input) {
        int start = input.indexOf('{');
        if (start < 0) {
            return null;
        }

        int depth = 0;
        for (int i = start; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch == '{') {
                depth++;
            } else if (ch == '}') {
                depth--;
                if (depth == 0) {
                    return input.substring(start, i + 1);
                }
            }
        }

        return null;
    }

    private long countOccurrences(String text, String token) {
        if (text == null || token == null || token.isBlank()) {
            return 0;
        }

        long count = 0;
        int index = 0;
        while ((index = text.indexOf(token, index)) != -1) {
            count++;
            index += token.length();
        }
        return count;
    }

    private boolean isStopWord(String token) {
        return switch (token) {
            case "with", "from", "that", "this", "have", "will", "your", "role", "team", "work", "and", "for", "the", "our", "you" -> true;
            default -> false;
        };
    }

    @PreDestroy
    public void shutdownExecutor() {
        aiExecutor.shutdownNow();
    }
}
