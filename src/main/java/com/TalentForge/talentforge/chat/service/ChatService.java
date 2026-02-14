package com.TalentForge.talentforge.chat.service;

import com.TalentForge.talentforge.chat.dto.ChatMessageResponse;
import com.TalentForge.talentforge.chat.dto.ChatRequest;

import java.util.List;

public interface ChatService {
    ChatMessageResponse ask(ChatRequest request);

    List<ChatMessageResponse> history(Long userId);
}
