package com.TalentForge.talentforge.admin.settings.service;

import com.TalentForge.talentforge.admin.settings.dto.AppSettingItemRequest;
import com.TalentForge.talentforge.admin.settings.dto.AppSettingResponse;
import com.TalentForge.talentforge.admin.settings.entity.AppSetting;
import com.TalentForge.talentforge.admin.settings.repository.AppSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppSettingServiceImpl implements AppSettingService {

    private final AppSettingRepository appSettingRepository;

    @Override
    public List<AppSettingResponse> getAll() {
        return appSettingRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<AppSettingResponse> upsert(List<AppSettingItemRequest> settings) {
        settings.forEach(item -> {
            String key = item.key().trim();
            String value = item.value().trim();
            String description = item.description() == null || item.description().isBlank() ? null : item.description().trim();

            AppSetting existing = appSettingRepository.findByKeyName(key).orElse(null);
            if (existing == null) {
                appSettingRepository.save(AppSetting.builder()
                        .keyName(key)
                        .valueText(value)
                        .description(description)
                        .build());
                return;
            }

            existing.setValueText(value);
            existing.setDescription(description);
            appSettingRepository.save(existing);
        });

        return getAll();
    }

    private AppSettingResponse toResponse(AppSetting setting) {
        return new AppSettingResponse(
                setting.getId(),
                setting.getKeyName(),
                setting.getValueText(),
                setting.getDescription(),
                setting.getCreatedAt(),
                setting.getUpdatedAt()
        );
    }
}
