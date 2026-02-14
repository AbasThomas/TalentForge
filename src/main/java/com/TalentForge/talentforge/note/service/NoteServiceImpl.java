package com.TalentForge.talentforge.note.service;

import com.TalentForge.talentforge.application.entity.Application;
import com.TalentForge.talentforge.application.repository.ApplicationRepository;
import com.TalentForge.talentforge.common.exception.ResourceNotFoundException;
import com.TalentForge.talentforge.note.dto.NoteRequest;
import com.TalentForge.talentforge.note.dto.NoteResponse;
import com.TalentForge.talentforge.note.entity.Note;
import com.TalentForge.talentforge.note.mapper.NoteMapper;
import com.TalentForge.talentforge.note.repository.NoteRepository;
import com.TalentForge.talentforge.user.entity.User;
import com.TalentForge.talentforge.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NoteServiceImpl implements NoteService {

    private final NoteRepository noteRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final NoteMapper noteMapper;

    @Override
    public NoteResponse create(NoteRequest request) {
        Application application = applicationRepository.findById(request.applicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + request.applicationId()));

        User recruiter = userRepository.findById(request.recruiterId())
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter not found: " + request.recruiterId()));

        Note note = Note.builder()
                .application(application)
                .recruiter(recruiter)
                .content(request.content())
                .build();

        return noteMapper.toResponse(noteRepository.save(note));
    }

    @Override
    public List<NoteResponse> getByApplication(Long applicationId) {
        return noteRepository.findByApplicationIdOrderByCreatedAtDesc(applicationId)
                .stream()
                .map(noteMapper::toResponse)
                .toList();
    }
}
