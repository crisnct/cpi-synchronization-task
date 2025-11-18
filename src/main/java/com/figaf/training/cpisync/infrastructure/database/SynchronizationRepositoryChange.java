package com.figaf.training.cpisync.infrastructure.database;

import com.figaf.training.cpisync.domain.SyncedObjectVersion;
import com.figaf.training.cpisync.application.dto.SynchronizationActionType;
import java.util.Objects;

public record SynchronizationRepositoryChange(
    SynchronizationActionType action,
    SyncedObjectVersion version
) {

    public SynchronizationRepositoryChange {
        Objects.requireNonNull(action, "Synchronization action must not be null");
        Objects.requireNonNull(version, "Synced object version must not be null");
    }
}


