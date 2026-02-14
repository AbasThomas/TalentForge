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

        String aiReply = aiAssistantService.generateChatReply(request.message());
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
}
