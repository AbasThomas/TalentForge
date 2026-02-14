package com.TalentForge.talentforge.chat.mapper;

import com.TalentForge.talentforge.chat.dto.ChatMessageResponse;
import com.TalentForge.talentforge.chat.entity.ChatMessage;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageMapper {

    public ChatMessageResponse toResponse(ChatMessage chatMessage) {
        return new ChatMessageResponse(
                chatMessage.getId(),
                chatMessage.getUserId(),
                chatMessage.getSenderType(),
                chatMessage.getMessage(),
                chatMessage.getSentAt()
        );
    }
}
