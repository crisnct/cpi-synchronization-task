package com.figaf.training.cpisync.infrastructure.database.entities;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "synced_object_version",
    uniqueConstraints = @UniqueConstraint(name = "uk_version_history_number", columnNames = {"history_id", "version_number"})
)
public class SyncedObjectVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "history_id", nullable = false)
    private SyncedObjectHistoryEntity history;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Column(name = "modification_date")
    private Instant modificationDate;

    @Column(name = "remote_version", length = 255)
    private String remoteVersion;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "synchronized_at", nullable = false)
    private Instant synchronizedAt;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "valid_to")
    private Instant validTo;

    @Column(name = "display_name", length = 512)
    private String displayName;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(name = "parent_technical_name", length = 255)
    private String parentTechnicalName;

    @Column(name = "parent_external_id", length = 255)
    private String parentExternalId;

    @Column(name = "creation_date")
    private Instant creationDate;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "modified_by", length = 255)
    private String modifiedBy;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "payload")
    private byte[] payload;

    @Column(name = "payload_content_type", length = 255)
    private String payloadContentType;

    @Column(name = "payload_file_name", length = 512)
    private String payloadFileName;

    @Column(name = "payload_size")
    private Long payloadSize;
}
