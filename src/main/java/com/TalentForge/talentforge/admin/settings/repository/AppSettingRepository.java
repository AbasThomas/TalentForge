package com.TalentForge.talentforge.admin.settings.repository;

import com.TalentForge.talentforge.admin.settings.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {
    Optional<AppSetting> findByKeyName(String keyName);
}
