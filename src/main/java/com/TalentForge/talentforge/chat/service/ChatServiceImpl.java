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
        if (normalized.contains("hello") || normalized.contains("hi")) {
            return "The AI model is currently unavailable, but I can still help. Ask about posting jobs, reviewing applications, interview scheduling, or resume scores.";
        }
        if (normalized.contains("interview") || normalized.contains("schedule")) {
            return "For interview workflow: open Applications, select a candidate, then use Advance to Interview to set date, format, and meeting link.";
        }
        if (normalized.contains("application") || normalized.contains("status")) {
            return "Use Recruiter > Applications to filter by job, then update each candidate status (REVIEWING, SHORTLISTED, INTERVIEWED, OFFERED, REJECTED, HIRED).";
        }
        if (normalized.contains("resume") || normalized.contains("score") || normalized.contains("cv")) {
            return "Resume scoring appears on each application detail with AI score, matched skills, and reasoning. Open a candidate record to review it.";
        }
        if (normalized.contains("job") || normalized.contains("post") || normalized.contains("publish")) {
            return "Create jobs from Recruiter > Jobs > New. After saving, use Recruiter > Integrations to share the public job link.";
        }
        if (normalized.contains("candidate")) {
            return "You can review candidates from Recruiter > Applications, then drill into one profile for resume, score, notes, and interview actions.";
        }

        return "The AI model is currently unavailable. I can still guide product workflows. Try: 'how do I post a job', 'how do I shortlist candidates', or 'how do I schedule an interview'.";
    }
}
