package com.TalentForge.talentforge.chat.service;

import com.TalentForge.talentforge.ai.service.AiAssistantService;
import com.TalentForge.talentforge.chat.dto.ChatMessageResponse;
import com.TalentForge.talentforge.chat.dto.ChatRequest;
import com.TalentForge.talentforge.chat.entity.ChatMessage;
import com.TalentForge.talentforge.chat.entity.SenderType;
import com.TalentForge.talentforge.chat.mapper.ChatMessageMapper;
import com.TalentForge.talentforge.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageMapper chatMessageMapper;
    private final AiAssistantService aiAssistantService;

    @Override
    public ChatMessageResponse ask(ChatRequest request) {
        ChatMessage userMessage = ChatMessage.builder()
                .userId(request.userId())
                .senderType(SenderType.USER)
                .message(request.message())
                .build();
        chatMessageRepository.save(userMessage);

        String aiReply;
        try {
            aiReply = aiAssistantService.generateChatReply(request.message());
        } catch (Exception ex) {
            log.warn("AI chat unavailable for userId={}. Falling back to deterministic reply. reason={}",
                    request.userId(),
                    ex.getMessage());
            log.debug("AI chat failure stacktrace for userId={}", request.userId(), ex);
            aiReply = fallbackReply(request.message());
        }
        ChatMessage botMessage = ChatMessage.builder()
                .userId(request.userId())
                .senderType(SenderType.BOT)
                .message(aiReply)
                .build();

        return chatMessageMapper.toResponse(chatMessageRepository.save(botMessage));
    }

    @Override
    public List<ChatMessageResponse> history(Long userId) {
        return chatMessageRepository.findByUserIdOrderBySentAtAsc(userId)
                .stream()
                .map(chatMessageMapper::toResponse)
                .toList();
    }

    private String fallbackReply(String message) {
        String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT);

        if (normalized.contains("hello") || normalized.contains("hi") || normalized.contains("hey")) {
            return "Hi there! I'm the TalentForge AI assistant. I'm having a brief connectivity issue right now, but I can still guide you through the platform.\n\nTry asking me:\n• **How do I post a job?**\n• **How do I review applicants?**\n• **How do I schedule an interview?**\n• **How do I improve my resume?**";
        }
        if (normalized.contains("interview") || normalized.contains("schedule")) {
            return "**Scheduling an interview on TalentForge:**\n\n1. Go to **Recruiter → Applications**\n2. Click on the candidate you want to interview\n3. Click **Schedule Interview**\n4. Set the date, time, interview type (Phone / Video / In-Person), and paste a meeting link if needed\n5. The candidate will see the interview in their **Applications → Interviews** page\n\nYou can also add feedback and update the interview status after it's completed.";
        }
        if (normalized.contains("application") || normalized.contains("status") || normalized.contains("pipeline")) {
            return "**Managing applications:**\n\n• Go to **Recruiter → Applications** to see all candidates across your jobs\n• Use the job filter to narrow down by a specific posting\n• Click any candidate to open their full AI analysis\n• Use the **Status** dropdown on their profile to move them through the pipeline:\n  `PENDING → REVIEWING → SHORTLISTED → INTERVIEWING → OFFERED → REJECTED`\n\nEach status change is saved instantly.";
        }
        if (normalized.contains("resume") || normalized.contains("score") || normalized.contains("cv") || normalized.contains("ai score")) {
            return "**About AI Resume Scoring:**\n\nEvery application receives an AI score from **0 to 100** based on:\n• Skill alignment with job requirements (40%)\n• Relevant experience evidence (30%)\n• Domain relevance (20%)\n• Communication clarity (10%)\n\nTo view a score: **Recruiter → Applications → [Candidate Name]**\n\nYou'll see the score, matched keywords, and the AI's reasoning. You can also click **Refresh AI Score** to re-run the analysis.";
        }
        if (normalized.contains("job") || normalized.contains("post") || normalized.contains("publish") || normalized.contains("create")) {
            return "**Creating a job posting:**\n\n1. Go to **Recruiter → Jobs → New Job**\n2. Fill in the **Role Details** — title, department, location, description, requirements\n3. Set **Compensation & Type** — salary range, job type, experience level\n4. Choose **Status**: Open (visible to candidates) or Draft (hidden)\n5. Optionally set a closing date\n6. Click **Post Job**\n\nOnce live, you can share the public link or post to social channels via **Integrations**.";
        }
        if (normalized.contains("candidate") || normalized.contains("applicant") || normalized.contains("pool")) {
            return "**Finding candidates on TalentForge:**\n\n• **Recruiter → Candidates** — browse all registered applicants, search by name, skills, or location, and view their AI score\n• **Recruiter → Applications** — see everyone who applied to your specific job postings\n• Click any candidate to view their full profile: resume, AI score, matched skills, cover letter, processing logs, notes, and interviews.";
        }
        if (normalized.contains("note") || normalized.contains("feedback")) {
            return "**Adding recruiter notes:**\n\n1. Go to **Recruiter → Applications**\n2. Open a candidate's profile\n3. Click **Recruiter Notes**\n4. Type your internal note and click **Add Note**\n\nNotes are private — only visible to your recruiting team, not the candidate. They appear newest-first.";
        }
        if (normalized.contains("subscription") || normalized.contains("plan") || normalized.contains("billing") || normalized.contains("payment")) {
            return "**Subscription & Billing:**\n\n• Go to **Recruiter → Subscription** or **Candidate → Subscription** to view your current plan\n• Plans: **Free**, **Starter**, **Pro**, **Enterprise** — each with different job post and applicant limits\n• Payments are processed via **Paystack** (card, bank transfer, mobile money, and more)\n• After payment, the page verifies your reference and activates your plan automatically\n\nNeed to upgrade? Click **Open Invoice** to start the payment flow.";
        }

        return "I'm having a brief connectivity issue right now, but I'm still here to help guide you.\n\nHere are some things you can ask me:\n• **How do I post a job?**\n• **How do I review and score applicants?**\n• **How do I schedule an interview?**\n• **How do I update a candidate's status?**\n• **How do I improve my resume?**\n• **Tips for writing better job descriptions**";
    }
}
