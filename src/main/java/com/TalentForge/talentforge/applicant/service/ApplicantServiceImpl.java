package com.TalentForge.talentforge.applicant.service;

import com.TalentForge.talentforge.applicant.dto.ApplicantRequest;
import com.TalentForge.talentforge.applicant.dto.ApplicantResponse;
import com.TalentForge.talentforge.applicant.entity.Applicant;
import com.TalentForge.talentforge.applicant.mapper.ApplicantMapper;
import com.TalentForge.talentforge.applicant.repository.ApplicantRepository;
import com.TalentForge.talentforge.common.exception.BadRequestException;
import com.TalentForge.talentforge.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApplicantServiceImpl implements ApplicantService {

    private final ApplicantRepository applicantRepository;
    private final ApplicantMapper applicantMapper;

    @Override
    public ApplicantResponse create(ApplicantRequest request) {
        if (applicantRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Applicant email already exists");
        }

        Applicant applicant = Applicant.builder()
                .fullName(request.fullName())
                .email(request.email())
                .phone(request.phone())
                .location(request.location())
                .linkedinUrl(request.linkedinUrl())
                .portfolioUrl(request.portfolioUrl())
                .summary(request.summary())
                .skills(request.skills())
                .yearsOfExperience(request.yearsOfExperience())
                .build();

        return applicantMapper.toResponse(applicantRepository.save(applicant));
    }

    @Override
    public ApplicantResponse update(Long id, ApplicantRequest request) {
        Applicant applicant = applicantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Applicant not found: " + id));

        applicant.setFullName(request.fullName());
        applicant.setPhone(request.phone());
        applicant.setLocation(request.location());
        applicant.setLinkedinUrl(request.linkedinUrl());
        applicant.setPortfolioUrl(request.portfolioUrl());
        applicant.setSummary(request.summary());
        applicant.setSkills(request.skills());
        applicant.setYearsOfExperience(request.yearsOfExperience());

        return applicantMapper.toResponse(applicantRepository.save(applicant));
    }

    @Override
    public ApplicantResponse getById(Long id) {
        Applicant applicant = applicantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Applicant not found: " + id));
        return applicantMapper.toResponse(applicant);
    }

    @Override
    public List<ApplicantResponse> getAll() {
        return applicantRepository.findAll().stream().map(applicantMapper::toResponse).toList();
    }

    @Override
    public void delete(Long id) {
        Applicant applicant = applicantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Applicant not found: " + id));
        applicantRepository.delete(applicant);
    }
}
