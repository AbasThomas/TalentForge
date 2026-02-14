package com.TalentForge.talentforge.ai.service;

import com.TalentForge.talentforge.ai.dto.AiResumeScoreResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OllamaAiAssistantService implements AiAssistantService {

    private final ChatClient.Builder chatClientBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExecutorService aiExecutor = Executors.newCachedThreadPool();

    @Override
    public String checkJobBias(String title, String description, String requirements) {
        try {
            String prompt = """
                    You are a hiring fairness assistant.
                    Review the job post below for bias and return:
                    1) Risk level (LOW, MEDIUM, HIGH)
                    2) Specific biased phrases
                    3) Inclusive rewrite suggestions

                    Title: %s
                    Description: %s
                    Requirements: %s
                    """.formatted(title, description, requirements == null ? "" : requirements);

            return callAi(prompt);
        } catch (Exception ex) {
            return "Bias check unavailable. Please review language manually for age, gender, and cultural exclusions.";
        }
    }

    @Override
    public AiResumeScoreResult scoreResume(String jobText, String resumeText) {
        try {
            String prompt = """
                    Score this resume against the job description from 0-100.
                    Return ONLY valid JSON in this shape:
                    {
                      "score": 0,
                      "reason": "short reason",
                      "matchingKeywords": "comma,separated,keywords"
                    }

                    JOB DESCRIPTION:
                    %s

                    RESUME TEXT:
                    %s
                    """.formatted(jobText, resumeText);

            String raw = callAi(prompt);
            JsonNode node = objectMapper.readTree(raw);
            double score = node.path("score").asDouble(0);
            String reason = node.path("reason").asText("No reason returned");
            String matchingKeywords = node.path("matchingKeywords").asText("");
            return new AiResumeScoreResult(Math.max(0, Math.min(100, score)), reason, matchingKeywords);
        } catch (Exception ex) {
            return fallbackScore(jobText, resumeText);
        }
    }

    @Override
    public String generateChatReply(String message) {
        String prompt = """
                You are HireSpark assistant for candidates and recruiters.
                Keep answers practical and concise.
                User message: %s
                """.formatted(message);

        try {
            return callAi(prompt);
        } catch (Exception ex) {
            return "I can help with job applications, status updates, and hiring questions. AI response is temporarily unavailable.";
        }
    }

    private String callAi(String prompt) {
        Future<String> future = aiExecutor.submit(() -> chatClientBuilder.build()
                .prompt()
                .user(prompt)
                .call()
                .content());

        try {
            return future.get(12, TimeUnit.SECONDS);
        } catch (Exception ex) {
            future.cancel(true);
            throw new RuntimeException("AI call timed out or failed", ex);
        }
    }

    private AiResumeScoreResult fallbackScore(String jobText, String resumeText) {
        String normalizedJob = jobText == null ? "" : jobText.toLowerCase(Locale.ROOT);
        String normalizedResume = resumeText == null ? "" : resumeText.toLowerCase(Locale.ROOT);

        Set<String> keywords = Arrays.stream(normalizedJob.split("[^a-z0-9+#]+"))
                .filter(token -> token.length() >= 4)
                .limit(40)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> matched = keywords.stream()
                .filter(normalizedResume::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        double score = keywords.isEmpty() ? 0 : ((double) matched.size() / keywords.size()) * 100;
        String matching = String.join(", ", matched.stream().limit(15).toList());
        String reason = "Fallback scoring used due to AI parsing failure. Matched " + matched.size() + " key terms.";

        return new AiResumeScoreResult(Math.round(score * 10.0) / 10.0, reason, matching);
    }

    @PreDestroy
    public void shutdownExecutor() {
        aiExecutor.shutdownNow();
    }
}
