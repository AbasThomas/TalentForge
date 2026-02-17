package com.TalentForge.talentforge.applicant.service;

import com.TalentForge.talentforge.applicant.dto.ApplicantRequest;
import com.TalentForge.talentforge.applicant.dto.ResumeScoreHistoryItemResponse;
import com.TalentForge.talentforge.applicant.dto.ApplicantResumeScoreRequest;
import com.TalentForge.talentforge.applicant.dto.ApplicantResumeScoreResponse;
import com.TalentForge.talentforge.applicant.dto.ApplicantResponse;
import com.TalentForge.talentforge.applicant.dto.ResumeScoreTaskResponse;
import com.TalentForge.talentforge.applicant.dto.ResumeScoreTaskSubmitResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ApplicantService {
    ApplicantResponse create(ApplicantRequest request);

    ApplicantResponse update(Long id, ApplicantRequest request);

    ApplicantResponse getById(Long id);

    List<ApplicantResponse> getAll();

    void delete(Long id);

    ApplicantResumeScoreResponse scoreResume(String userEmail, ApplicantResumeScoreRequest request, MultipartFile resumeFile);

    List<ResumeScoreHistoryItemResponse> getResumeScoreHistory(String userEmail);

    ResumeScoreTaskSubmitResponse submitResumeScoreTask(String userEmail, ApplicantResumeScoreRequest request, MultipartFile resumeFile);

    ResumeScoreTaskResponse getResumeScoreTask(String userEmail, Long taskId);

    List<ResumeScoreTaskResponse> getResumeScoreTasks(String userEmail);
}
