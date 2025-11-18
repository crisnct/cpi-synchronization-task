package com.figaf.training.cpisync.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.figaf.training.cpisync.application.dto.SyncedObjectLatestVersionResponse;
import com.figaf.training.cpisync.application.dto.SyncedObjectVersionPayload;
import com.figaf.training.cpisync.application.service.SyncedObjectDescriptor;
import com.figaf.training.cpisync.domain.SyncedObjectVersion;
import com.figaf.training.cpisync.infrastructure.database.entities.SyncedObjectHistoryEntity;
import com.figaf.training.cpisync.infrastructure.database.entities.SyncedObjectVersionEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SynchronizationMapper {

    @Autowired
    private ObjectMapper objectMapper;

    public SyncedObjectLatestVersionResponse toResponse(SyncedObjectDescriptor descriptor, SyncedObjectVersion version) {
        return new SyncedObjectLatestVersionResponse(
            descriptor.type(),
            descriptor.technicalName(),
            version.getVersionNumber(),
            version.isDeleted(),
            version.getSynchronizedAt(),
            version.getModificationDate().orElse(null),
            version.getRemoteVersion().orElse(null),
            version.getDisplayName().orElse(null),
            version.getExternalId().orElse(null),
            version.getParentTechnicalName().orElse(null),
            version.getParentExternalId().orElse(null),
            version.getCreationDate().orElse(null),
            version.getCreatedBy().orElse(null),
            version.getModifiedBy().orElse(null),
            version.getPayloadContentType().orElse(null),
            version.getPayloadFileName().orElse(null),
            version.getPayloadSize().orElse(null)
        );
    }

    public SyncedObjectVersionPayload toPayloadFromExisting(SyncedObjectVersion version, boolean includePayload) {
        SyncedObjectVersionPayload.Builder builder = SyncedObjectVersionPayload.builder();
        version.getModificationDate().ifPresent(builder::modificationDate);
        version.getRemoteVersion().ifPresent(builder::remoteVersion);
        version.getDisplayName().ifPresent(builder::displayName);
        version.getExternalId().ifPresent(builder::externalId);
        version.getParentTechnicalName().ifPresent(builder::parentTechnicalName);
        version.getParentExternalId().ifPresent(builder::parentExternalId);
        version.getCreationDate().ifPresent(builder::creationDate);
        version.getCreatedBy().ifPresent(builder::createdBy);
        version.getModifiedBy().ifPresent(builder::modifiedBy);
        if (includePayload) {
            version.getPayload().ifPresent(builder::payload);
            version.getPayloadContentType().ifPresent(builder::payloadContentType);
            version.getPayloadFileName().ifPresent(builder::payloadFileName);
            version.getPayloadSize().ifPresent(builder::payloadSize);
        }
        return builder.build();
    }

    public SyncedObjectVersionEntity toEntity(SyncedObjectHistoryEntity history, SyncedObjectVersion version) {
        SyncedObjectVersionEntity entity = new SyncedObjectVersionEntity();
        entity.setHistory(history);
        entity.setVersionNumber(version.getVersionNumber());
        entity.setModificationDate(version.getModificationDate().orElse(null));
        entity.setRemoteVersion(version.getRemoteVersion().orElse(null));
        entity.setDeleted(version.isDeleted());
        entity.setSynchronizedAt(version.getSynchronizedAt());
        entity.setDisplayName(version.getDisplayName().orElse(null));
        entity.setExternalId(version.getExternalId().orElse(null));
        entity.setParentTechnicalName(version.getParentTechnicalName().orElse(null));
        entity.setParentExternalId(version.getParentExternalId().orElse(null));
        entity.setCreationDate(version.getCreationDate().orElse(null));
        entity.setCreatedBy(version.getCreatedBy().orElse(null));
        entity.setModifiedBy(version.getModifiedBy().orElse(null));
        entity.setPayload(version.getPayload().orElse(null));
        entity.setPayloadContentType(version.getPayloadContentType().orElse(null));
        entity.setPayloadFileName(version.getPayloadFileName().orElse(null));
        entity.setPayloadSize(version.getPayloadSize().orElse(null));
        return entity;
    }

    public SyncedObjectVersion toDomain(SyncedObjectVersionEntity entity, boolean includePayload) {
        SyncedObjectVersion.Builder builder = SyncedObjectVersion.builder()
            .versionNumber(entity.getVersionNumber())
            .deleted(entity.isDeleted())
            .synchronizedAt(entity.getSynchronizedAt());

        if (entity.getModificationDate() != null) {
            builder.modificationDate(entity.getModificationDate());
        }
        if (entity.getRemoteVersion() != null) {
            builder.remoteVersion(entity.getRemoteVersion());
        }
        if (entity.getDisplayName() != null) {
            builder.displayName(entity.getDisplayName());
        }
        if (entity.getExternalId() != null) {
            builder.externalId(entity.getExternalId());
        }
        if (entity.getParentTechnicalName() != null) {
            builder.parentTechnicalName(entity.getParentTechnicalName());
        }
        if (entity.getParentExternalId() != null) {
            builder.parentExternalId(entity.getParentExternalId());
        }
        if (entity.getCreationDate() != null) {
            builder.creationDate(entity.getCreationDate());
        }
        if (entity.getCreatedBy() != null) {
            builder.createdBy(entity.getCreatedBy());
        }
        if (entity.getModifiedBy() != null) {
            builder.modifiedBy(entity.getModifiedBy());
        }
        if (includePayload) {
            byte[] payload = entity.getPayload();
            if (payload != null) {
                builder.payloadRaw(payload);
            }
        }
        if (entity.getPayloadContentType() != null) {
            builder.payloadContentType(entity.getPayloadContentType());
        }
        if (entity.getPayloadFileName() != null) {
            builder.payloadFileName(entity.getPayloadFileName());
        }
        if (entity.getPayloadSize() != null) {
            builder.payloadSize(entity.getPayloadSize());
        }
        return builder.build();
    }

    public byte[] serializeToByteArray(Object obj) throws JsonProcessingException {
        return objectMapper.writeValueAsBytes(obj);
    }

}
