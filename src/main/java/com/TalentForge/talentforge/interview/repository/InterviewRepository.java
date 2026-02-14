package com.TalentForge.talentforge.interview.repository;

import com.TalentForge.talentforge.interview.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
    List<Interview> findByApplicationId(Long applicationId);
}
