package com.TalentForge.talentforge.integration.service;

import com.TalentForge.talentforge.integration.dto.IntegrationBulkPublishRequest;
import com.TalentForge.talentforge.integration.dto.IntegrationConnectionRequest;
import com.TalentForge.talentforge.integration.dto.IntegrationConnectionResponse;
import com.TalentForge.talentforge.integration.dto.IntegrationPublishLogResponse;
import com.TalentForge.talentforge.integration.dto.IntegrationPublishRequest;
import com.TalentForge.talentforge.integration.dto.IntegrationPublishResponse;
import com.TalentForge.talentforge.integration.entity.IntegrationPlatform;

import java.util.List;

public interface IntegrationService {
    List<IntegrationConnectionResponse> getConnections(Long recruiterId);

    IntegrationConnectionResponse upsertConnection(IntegrationPlatform platform, IntegrationConnectionRequest request);

    void disconnect(Long recruiterId, IntegrationPlatform platform);

    IntegrationPublishResponse publish(IntegrationPublishRequest request);

    List<IntegrationPublishResponse> publishAll(IntegrationBulkPublishRequest request);

    List<IntegrationPublishLogResponse> getPublishLogs(Long recruiterId, Long jobId);
}
