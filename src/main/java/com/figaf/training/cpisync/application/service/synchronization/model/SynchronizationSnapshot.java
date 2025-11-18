package com.figaf.training.cpisync.application.service.synchronization.model;

import com.figaf.training.cpisync.application.dto.SynchronizationResultEntry;
import java.util.List;

public record SynchronizationSnapshot (
    SynchronizationSnapshotMetadata metadata,
    List<SynchronizationResultEntry> entries
) {}