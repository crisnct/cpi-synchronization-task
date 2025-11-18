package com.figaf.training.cpisync.application.dto;

import com.figaf.training.cpisync.domain.SyncedObjectVersion;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

public class SyncedObjectVersionPayload {

    private final Instant modificationDate;
    private final String remoteVersion;
    private final String displayName;
    private final String externalId;
    private final String parentTechnicalName;
    private final String parentExternalId;
    private final Instant creationDate;
    private final String createdBy;
    private final String modifiedBy;
    private final byte[] payload;
    private final String payloadContentType;
    private final String payloadFileName;
    private final Long payloadSize;

    private SyncedObjectVersionPayload(Builder builder) {
        this.modificationDate = builder.modificationDate;
        this.remoteVersion = builder.remoteVersion;
        this.displayName = builder.displayName;
        this.externalId = builder.externalId;
        this.parentTechnicalName = builder.parentTechnicalName;
        this.parentExternalId = builder.parentExternalId;
        this.creationDate = builder.creationDate;
        this.createdBy = builder.createdBy;
        this.modifiedBy = builder.modifiedBy;
        this.payload = builder.payload == null ? null : builder.payload.clone();
        this.payloadContentType = builder.payloadContentType;
        this.payloadFileName = builder.payloadFileName;
        this.payloadSize = builder.payloadSize;
    }

    public Optional<Instant> getModificationDate() {
        return Optional.ofNullable(modificationDate);
    }

    public Optional<String> getRemoteVersion() {
        return Optional.ofNullable(remoteVersion);
    }

    public Optional<String> getDisplayName() {
        return Optional.ofNullable(displayName);
    }

    public Optional<String> getExternalId() {
        return Optional.ofNullable(externalId);
    }

    public Optional<String> getParentTechnicalName() {
        return Optional.ofNullable(parentTechnicalName);
    }

    public Optional<String> getParentExternalId() {
        return Optional.ofNullable(parentExternalId);
    }

    public Optional<Instant> getCreationDate() {
        return Optional.ofNullable(creationDate);
    }

    public Optional<String> getCreatedBy() {
        return Optional.ofNullable(createdBy);
    }

    public Optional<String> getModifiedBy() {
        return Optional.ofNullable(modifiedBy);
    }

    public Optional<byte[]> getPayload() {
        return payload == null ? Optional.empty() : Optional.of(Arrays.copyOf(payload, payload.length));
    }

    public Optional<String> getPayloadContentType() {
        return Optional.ofNullable(payloadContentType);
    }

    public Optional<String> getPayloadFileName() {
        return Optional.ofNullable(payloadFileName);
    }

    public Optional<Long> getPayloadSize() {
        return Optional.ofNullable(payloadSize);
    }

    public void applyTo(SyncedObjectVersion.Builder builder) {
        if (builder == null) {
            return;
        }
        if (modificationDate != null) {
            builder.modificationDate(modificationDate);
        }
        if (remoteVersion != null) {
            builder.remoteVersion(remoteVersion);
        }
        if (displayName != null) {
            builder.displayName(displayName);
        }
        if (externalId != null) {
            builder.externalId(externalId);
        }
        if (parentTechnicalName != null) {
            builder.parentTechnicalName(parentTechnicalName);
        }
        if (parentExternalId != null) {
            builder.parentExternalId(parentExternalId);
        }
        if (creationDate != null) {
            builder.creationDate(creationDate);
        }
        if (createdBy != null) {
            builder.createdBy(createdBy);
        }
        if (modifiedBy != null) {
            builder.modifiedBy(modifiedBy);
        }
        if (payload != null) {
            builder.payloadRaw(payload);
        }
        if (payloadContentType != null) {
            builder.payloadContentType(payloadContentType);
        }
        if (payloadFileName != null) {
            builder.payloadFileName(payloadFileName);
        }
        if (payloadSize != null) {
            builder.payloadSize(payloadSize);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private Instant modificationDate;
        private String remoteVersion;
        private String displayName;
        private String externalId;
        private String parentTechnicalName;
        private String parentExternalId;
        private Instant creationDate;
        private String createdBy;
        private String modifiedBy;
        private byte[] payload;
        private String payloadContentType;
        private String payloadFileName;
        private Long payloadSize;

        private Builder() {
        }

        public Builder modificationDate(Instant modificationDate) {
            this.modificationDate = modificationDate;
            return this;
        }

        public Builder remoteVersion(String remoteVersion) {
            this.remoteVersion = remoteVersion;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder externalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        public Builder parentTechnicalName(String parentTechnicalName) {
            this.parentTechnicalName = parentTechnicalName;
            return this;
        }

        public Builder parentExternalId(String parentExternalId) {
            this.parentExternalId = parentExternalId;
            return this;
        }

        public Builder creationDate(Instant creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public Builder modifiedBy(String modifiedBy) {
            this.modifiedBy = modifiedBy;
            return this;
        }

        public Builder payload(byte[] payload) {
            this.payload = payload == null ? null : payload.clone();
            return this;
        }

        public Builder payloadContentType(String payloadContentType) {
            this.payloadContentType = payloadContentType;
            return this;
        }

        public Builder payloadFileName(String payloadFileName) {
            this.payloadFileName = payloadFileName;
            return this;
        }

        public Builder payloadSize(Long payloadSize) {
            this.payloadSize = payloadSize;
            return this;
        }

        public SyncedObjectVersionPayload build() {
            if (payload != null && payloadSize == null) {
                payloadSize = (long) payload.length;
            }
            return new SyncedObjectVersionPayload(this);
        }
    }
}

