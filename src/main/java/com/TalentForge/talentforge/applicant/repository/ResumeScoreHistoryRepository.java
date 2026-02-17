package com.TalentForge.talentforge.applicant.repository;

import com.TalentForge.talentforge.applicant.entity.ResumeScoreHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeScoreHistoryRepository extends JpaRepository<ResumeScoreHistory, Long> {
    List<ResumeScoreHistory> findTop100ByUserIdOrderByCreatedAtDesc(Long userId);
}
