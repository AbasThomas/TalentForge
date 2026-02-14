package com.TalentForge.talentforge.application.repository;

import com.TalentForge.talentforge.application.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByJobId(Long jobId);

    List<Application> findByApplicantId(Long applicantId);

    boolean existsByJobIdAndApplicantId(Long jobId, Long applicantId);
}
