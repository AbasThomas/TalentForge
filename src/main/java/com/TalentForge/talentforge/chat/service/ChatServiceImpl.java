package com.TalentForge.talentforge.chat.service;

import com.TalentForge.talentforge.ai.service.AiAssistantService;
import com.TalentForge.talentforge.chat.dto.ChatMessageResponse;
import com.TalentForge.talentforge.chat.dto.ChatRequest;
import com.TalentForge.talentforge.chat.entity.ChatMessage;
import com.TalentForge.talentforge.chat.entity.SenderType;
import com.TalentForge.talentforge.chat.mapper.ChatMessageMapper;
import com.TalentForge.talentforge.chat.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
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
        } catch (Exception ignored) {
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

        if (normalized.contains("status")) {
            return "Use Applications > filter by job to check current candidate statuses and interview stage.";
        }
        if (normalized.contains("resume") || normalized.contains("score")) {
            return "Resume AI scoring is available in each application detail under AI score, reasoning, and matched skills.";
        }
        if (normalized.contains("job") && normalized.contains("post")) {
            return "Create a job from Recruiter > Jobs > New, then publish via Recruiter > Integrations.";
        }
        return "AI model is temporarily unavailable. You can still manage jobs, applications, and interview workflows now.";
    }
}
