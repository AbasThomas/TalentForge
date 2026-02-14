package com.TalentForge.talentforge.note.service;

import com.TalentForge.talentforge.note.dto.NoteRequest;
import com.TalentForge.talentforge.note.dto.NoteResponse;

import java.util.List;

public interface NoteService {
    NoteResponse create(NoteRequest request);

    List<NoteResponse> getByApplication(Long applicationId);
}
