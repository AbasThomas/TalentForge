package com.TalentForge.talentforge.applicant.mapper;

import com.TalentForge.talentforge.applicant.dto.ApplicantResponse;
import com.TalentForge.talentforge.applicant.entity.Applicant;
import org.springframework.stereotype.Component;

@Component
public class ApplicantMapper {

    public ApplicantResponse toResponse(Applicant applicant) {
        return new ApplicantResponse(
                applicant.getId(),
                applicant.getFullName(),
                applicant.getEmail(),
                applicant.getPhone(),
                applicant.getLocation(),
                applicant.getLinkedinUrl(),
                applicant.getPortfolioUrl(),
                applicant.getSummary(),
                applicant.getSkills(),
                applicant.getYearsOfExperience(),
                applicant.getCreatedAt(),
                applicant.getUpdatedAt()
        );
    }
}
