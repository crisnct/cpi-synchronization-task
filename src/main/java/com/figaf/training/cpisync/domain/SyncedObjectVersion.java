package com.figaf.training.cpisync.domain;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;

public class SyncedObjectVersion {

    @Getter
    private final int versionNumber;

    /**
     * Example: 2025-01-18T11:59:12Z
     */
    private final Instant modificationDate;

    /**
     * Example: 1.0.3
     */
    private final String remoteVersion;

    @Getter
    private final boolean deleted;

    /**
     * Example: 2025-01-18T12:32:44Z
     */
    @Getter
    private final Instant synchronizedAt;

    /**
     * Example: enhanced SAPERP Invoice01_OA Inbound Processing
     */
    private final String displayName;

    /**
     * Example: fee2e3a17b4c4b1dad0bd9ab9284871a
     */
    private final String externalId;

    /**
     * Example: 2502AlphaVer
     */
    private final String parentTechnicalName;

    /**
     * Example: 45cf7f05898d459abd6f952016b1454c
     */
    private final String parentExternalId;

    /**
     * Example: 2024-12-01T08:15:00Z
     */
    private final Instant creationDate;

    /**
     * Example: john.doe@company.com
     */
    private final String createdBy;

    /**
     * Example: test-user@figaf.com
     */
    private final String modifiedBy;

    private final byte[] payload;

    /**
     * Example: application/zip
     */
    private final String payloadContentType;

    /**
     * Example: efbc124253d349aca8bc9ded793961a4.zip
     */
    private final String payloadFileName;

    /**
     * Example: 14320
     */
    private final Long payloadSize;

    private SyncedObjectVersion(Builder builder) {
        this.versionNumber = builder.versionNumber;
        this.modificationDate = builder.modificationDate;
        this.remoteVersion = builder.remoteVersion;
        this.deleted = builder.deleted;
        this.synchronizedAt = builder.synchronizedAt;
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int versionNumber;
        private Instant modificationDate;
        private String remoteVersion;
        private boolean deleted;
        private Instant synchronizedAt;
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

        public Builder versionNumber(int versionNumber) {
            this.versionNumber = versionNumber;
            return this;
        }

        public Builder modificationDate(Instant modificationDate) {
            this.modificationDate = modificationDate;
            return this;
        }

        public Builder remoteVersion(String remoteVersion) {
            this.remoteVersion = remoteVersion;
            return this;
        }

        public Builder deleted(boolean deleted) {
            this.deleted = deleted;
            return this;
        }

        public Builder synchronizedAt(Instant synchronizedAt) {
            this.synchronizedAt = synchronizedAt;
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

        /**
         * Internal helper for trusted callers that already hold an immutable copy of the payload.
         */
        public Builder payloadRaw(byte[] payload) {
            this.payload = payload;
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

        public SyncedObjectVersion build() {
            if (synchronizedAt == null) {
                synchronizedAt = Instant.now();
            }
            if (payload != null && payloadSize == null) {
                payloadSize = (long) payload.length;
            }
            return new SyncedObjectVersion(this);
        }
    }
}
