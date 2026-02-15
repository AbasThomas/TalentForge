package com.TalentForge.talentforge.integration.repository;

import com.TalentForge.talentforge.integration.entity.IntegrationPlatform;
import com.TalentForge.talentforge.integration.entity.RecruiterIntegration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecruiterIntegrationRepository extends JpaRepository<RecruiterIntegration, Long> {
    List<RecruiterIntegration> findByRecruiterIdOrderByPlatformAsc(Long recruiterId);

    List<RecruiterIntegration> findByRecruiterIdAndConnectedTrueOrderByPlatformAsc(Long recruiterId);

    Optional<RecruiterIntegration> findByRecruiterIdAndPlatform(Long recruiterId, IntegrationPlatform platform);
}
