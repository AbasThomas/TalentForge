package com.TalentForge.talentforge.application.service;

import com.TalentForge.talentforge.application.dto.ApplicationCreateRequest;
import com.TalentForge.talentforge.application.dto.ApplicationResponse;
import com.TalentForge.talentforge.application.entity.ApplicationStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ApplicationService {
    ApplicationResponse submit(ApplicationCreateRequest request, MultipartFile resumeFile);

    ApplicationResponse updateStatus(Long id, ApplicationStatus status);

    ApplicationResponse getById(Long id);

    List<ApplicationResponse> getByJobId(Long jobId);

    List<ApplicationResponse> getByApplicantId(Long applicantId);
}
