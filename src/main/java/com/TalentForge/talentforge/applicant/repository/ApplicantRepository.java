package com.TalentForge.talentforge.applicant.repository;

import com.TalentForge.talentforge.applicant.entity.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ApplicantRepository extends JpaRepository<Applicant, Long> {
    Optional<Applicant> findByEmail(String email);

    boolean existsByEmail(String email);
}
