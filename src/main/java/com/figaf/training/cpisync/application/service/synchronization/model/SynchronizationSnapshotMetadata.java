package com.figaf.training.cpisync.application.service.synchronization.model;

import java.time.Instant;
import java.util.UUID;

public record SynchronizationSnapshotMetadata(
    UUID jobId,
    SynchronizationJobStatus status,
    Instant startedAt,
    Instant finishedAt,
    long registeredCount,
    long updatedCount,
    long deletedCount,
    String errorMessage
) {}
