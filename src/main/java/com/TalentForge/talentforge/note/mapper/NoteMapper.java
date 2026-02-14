package com.TalentForge.talentforge.note.mapper;

import com.TalentForge.talentforge.note.dto.NoteResponse;
import com.TalentForge.talentforge.note.entity.Note;
import org.springframework.stereotype.Component;

@Component
public class NoteMapper {

    public NoteResponse toResponse(Note note) {
        return new NoteResponse(
                note.getId(),
                note.getApplication().getId(),
                note.getRecruiter().getId(),
                note.getRecruiter().getFullName(),
                note.getContent(),
                note.getCreatedAt()
        );
    }
}
