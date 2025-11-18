package com.figaf.training.cpisync.application.dto;

import com.figaf.training.cpisync.domain.SyncedObjectType;
import java.time.Instant;

public record SyncedObjectLatestVersionResponse(
    SyncedObjectType type,
    String technicalName,
    int versionNumber,
    boolean deleted,
    Instant synchronizedAt,
    Instant modificationDate,
    String remoteVersion,
    String displayName,
    String externalId,
    String parentTechnicalName,
    String parentExternalId,
    Instant creationDate,
    String createdBy,
    String modifiedBy,
    String payloadContentType,
    String payloadFileName,
    Long payloadSize
) {}

