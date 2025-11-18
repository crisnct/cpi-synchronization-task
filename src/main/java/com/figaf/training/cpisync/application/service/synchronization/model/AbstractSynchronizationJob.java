package com.figaf.training.cpisync.application.service.synchronization.model;

import com.figaf.training.cpisync.application.dto.SynchronizationResultEntry;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import org.apache.commons.lang3.exception.ExceptionUtils;

public abstract class AbstractSynchronizationJob implements SynchronizationJobProgressTracker, Runnable {

    @Getter
    private final UUID id = UUID.randomUUID();
    private final AtomicReference<SynchronizationJobStatus> status = new AtomicReference<>(SynchronizationJobStatus.QUEUED);
    private final AtomicReference<Instant> startedAt = new AtomicReference<>();
    private final AtomicReference<Instant> finishedAt = new AtomicReference<>();
    private final ConcurrentLinkedQueue<SynchronizationResultEntry> entries = new ConcurrentLinkedQueue<>();
    private final AtomicLong registeredCount = new AtomicLong();
    private final AtomicLong updatedCount = new AtomicLong();
    private final AtomicLong deletedCount = new AtomicLong();
    private final AtomicReference<String> errorMessage = new AtomicReference<>();

    public SynchronizationJobStatus getStatus() {
        return status.get();
    }

    @Override
    public void markStarted() {
        this.startedAt.compareAndSet(null, Instant.now());
        this.status.set(SynchronizationJobStatus.RUNNING);
    }

    @Override
    public void addEntry(SynchronizationResultEntry entry) {
        entries.add(entry);
        switch (entry.action()) {
            case REGISTERED -> registeredCount.incrementAndGet();
            case UPDATED -> updatedCount.incrementAndGet();
            case MARKED_AS_DELETED -> deletedCount.incrementAndGet();
        }
    }

    @Override
    public void markCompleted() {
        this.finishedAt.set(Instant.now());
        this.status.set(SynchronizationJobStatus.COMPLETED);
    }

    @Override
    public void markFailed(Throwable error) {
        this.finishedAt.set(Instant.now());
        this.status.set(SynchronizationJobStatus.FAILED);
        this.errorMessage.set(error == null ? null : ExceptionUtils.getMessage(error) + "\n" + ExceptionUtils.getStackTrace(error));
    }

    public SynchronizationSnapshot getFullSnapshot() {
        return new SynchronizationSnapshot(this.getMetadataSnapshot(), List.copyOf(entries));
    }

    public SynchronizationSnapshotMetadata getMetadataSnapshot() {
        return new SynchronizationSnapshotMetadata(
            id,
            status.get(),
            startedAt.get(),
            finishedAt.get(),
            registeredCount.get(),
            updatedCount.get(),
            deletedCount.get(),
            errorMessage.get()
        );
    }

}

