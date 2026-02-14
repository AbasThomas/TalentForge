package com.TalentForge.talentforge.applicant.service;

import com.TalentForge.talentforge.applicant.dto.ApplicantRequest;
import com.TalentForge.talentforge.applicant.dto.ApplicantResponse;

import java.util.List;

public interface ApplicantService {
    ApplicantResponse create(ApplicantRequest request);

    ApplicantResponse update(Long id, ApplicantRequest request);

    ApplicantResponse getById(Long id);

    List<ApplicantResponse> getAll();

    void delete(Long id);
}
