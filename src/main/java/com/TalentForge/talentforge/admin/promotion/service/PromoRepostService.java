package com.TalentForge.talentforge.admin.promotion.service;

import com.TalentForge.talentforge.admin.promotion.dto.PromoRepostResponse;
import com.TalentForge.talentforge.admin.promotion.entity.PromoRepostStatus;

import java.util.List;

public interface PromoRepostService {
    List<PromoRepostResponse> getAll();

    PromoRepostResponse setConsent(Long jobId, boolean consentGiven);

    PromoRepostResponse setStatus(Long jobId, PromoRepostStatus status);
}
