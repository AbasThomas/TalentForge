package com.TalentForge.talentforge.note.controller;

import com.TalentForge.talentforge.common.payload.ApiResponse;
import com.TalentForge.talentforge.note.dto.NoteRequest;
import com.TalentForge.talentforge.note.dto.NoteResponse;
import com.TalentForge.talentforge.note.service.NoteService;
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
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @PostMapping
    public ResponseEntity<ApiResponse<NoteResponse>> create(@Valid @RequestBody NoteRequest request) {
        return ResponseEntity.ok(ApiResponse.<NoteResponse>builder()
                .success(true)
                .message("Note created")
                .data(noteService.create(request))
                .build());
    }

    @GetMapping("/application/{applicationId}")
    public ResponseEntity<ApiResponse<List<NoteResponse>>> byApplication(@PathVariable Long applicationId) {
        return ResponseEntity.ok(ApiResponse.<List<NoteResponse>>builder()
                .success(true)
                .message("Notes fetched")
                .data(noteService.getByApplication(applicationId))
                .build());
    }
}
