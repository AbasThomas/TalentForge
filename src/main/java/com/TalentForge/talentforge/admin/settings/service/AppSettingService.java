package com.TalentForge.talentforge.admin.settings.service;

import com.TalentForge.talentforge.admin.settings.dto.AppSettingItemRequest;
import com.TalentForge.talentforge.admin.settings.dto.AppSettingResponse;

import java.util.List;

public interface AppSettingService {
    List<AppSettingResponse> getAll();

    List<AppSettingResponse> upsert(List<AppSettingItemRequest> settings);
}
