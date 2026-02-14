package com.TalentForge.talentforge.chat.controller;

import com.TalentForge.talentforge.chat.dto.ChatMessageResponse;
import com.TalentForge.talentforge.chat.dto.ChatRequest;
import com.TalentForge.talentforge.chat.service.ChatService;
import com.TalentForge.talentforge.common.payload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ApiResponse<ChatMessageResponse>> ask(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(ApiResponse.<ChatMessageResponse>builder()
                .success(true)
                .message("Chat response generated")
                .data(chatService.ask(request))
                .build());
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> history(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.<List<ChatMessageResponse>>builder()
                .success(true)
                .message("Chat history fetched")
                .data(chatService.history(userId))
                .build());
    }
}
