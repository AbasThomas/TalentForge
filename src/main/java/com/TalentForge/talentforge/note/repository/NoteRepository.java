package com.TalentForge.talentforge.note.repository;

import com.TalentForge.talentforge.note.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoteRepository extends JpaRepository<Note, Long> {
    List<Note> findByApplicationIdOrderByCreatedAtDesc(Long applicationId);
}
