package com.TalentForge.talentforge.admin.promotion.repository;

import com.TalentForge.talentforge.admin.promotion.entity.PromoRepost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromoRepostRepository extends JpaRepository<PromoRepost, Long> {
    Optional<PromoRepost> findByJobId(Long jobId);
}
