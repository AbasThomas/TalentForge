package com.TalentForge.talentforge.applicant.repository;

import com.TalentForge.talentforge.applicant.entity.ResumeScoreTask;
import com.TalentForge.talentforge.applicant.entity.ResumeScoreTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ResumeScoreTaskRepository extends JpaRepository<ResumeScoreTask, Long> {
    Optional<ResumeScoreTask> findByIdAndUserId(Long id, Long userId);

    List<ResumeScoreTask> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndStatusIn(Long userId, Collection<ResumeScoreTaskStatus> statuses);
}
