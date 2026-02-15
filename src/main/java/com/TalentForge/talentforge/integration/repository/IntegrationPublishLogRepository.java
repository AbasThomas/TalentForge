package com.TalentForge.talentforge.integration.repository;

import com.TalentForge.talentforge.integration.entity.IntegrationPublishLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IntegrationPublishLogRepository extends JpaRepository<IntegrationPublishLog, Long> {
    List<IntegrationPublishLog> findTop50ByRecruiterIdOrderByCreatedAtDesc(Long recruiterId);

    List<IntegrationPublishLog> findTop50ByRecruiterIdAndJobIdOrderByCreatedAtDesc(Long recruiterId, Long jobId);
}
