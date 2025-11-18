package com.figaf.training.cpisync.application.dto;

import com.figaf.training.cpisync.domain.SyncedObjectType;
import java.time.Instant;

public record SynchronizationResultEntry(
    SyncedObjectType type,
    String technicalName,
    SynchronizationActionType action,
    int version,
    boolean deleted,
    Instant synchronizedAt
) { }

