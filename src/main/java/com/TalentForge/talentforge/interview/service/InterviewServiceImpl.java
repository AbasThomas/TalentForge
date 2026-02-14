package com.TalentForge.talentforge.interview.service;

import com.TalentForge.talentforge.application.entity.Application;
import com.TalentForge.talentforge.application.entity.ApplicationStatus;
import com.TalentForge.talentforge.application.repository.ApplicationRepository;
import com.TalentForge.talentforge.common.exception.ResourceNotFoundException;
import com.TalentForge.talentforge.interview.dto.InterviewRequest;
import com.TalentForge.talentforge.interview.dto.InterviewResponse;
import com.TalentForge.talentforge.interview.entity.Interview;
import com.TalentForge.talentforge.interview.entity.InterviewStatus;
import com.TalentForge.talentforge.interview.mapper.InterviewMapper;
import com.TalentForge.talentforge.interview.repository.InterviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewMapper interviewMapper;

    @Override
    public InterviewResponse create(InterviewRequest request) {
        Application application = applicationRepository.findById(request.applicationId())
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + request.applicationId()));

        Interview interview = Interview.builder()
                .application(application)
                .scheduledAt(request.scheduledAt())
                .interviewType(request.interviewType())
                .meetingLink(request.meetingLink())
                .status(request.status() == null ? InterviewStatus.SCHEDULED : request.status())
                .feedback(request.feedback())
                .build();

        if (interview.getStatus() == InterviewStatus.COMPLETED) {
            application.setStatus(ApplicationStatus.INTERVIEWED);
            application.setInterviewedAt(LocalDateTime.now());
            applicationRepository.save(application);
        }

        return interviewMapper.toResponse(interviewRepository.save(interview));
    }

    @Override
    public List<InterviewResponse> getByApplication(Long applicationId) {
        return interviewRepository.findByApplicationId(applicationId).stream().map(interviewMapper::toResponse).toList();
    }
}
