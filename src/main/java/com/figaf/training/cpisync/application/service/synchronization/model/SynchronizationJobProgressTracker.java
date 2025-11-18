package com.figaf.training.cpisync.application.service.synchronization.model;

import com.figaf.training.cpisync.application.dto.SynchronizationResultEntry;

public interface SynchronizationJobProgressTracker {

    void markStarted();

    void addEntry(SynchronizationResultEntry entry);

    void markCompleted();

    void markFailed(Throwable error);
}

