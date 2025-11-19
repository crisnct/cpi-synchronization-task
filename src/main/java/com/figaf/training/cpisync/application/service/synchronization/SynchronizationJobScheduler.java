package com.figaf.training.cpisync.application.service.synchronization;

import com.figaf.training.cpisync.application.service.synchronization.model.AbstractSynchronizationJob;
import com.figaf.training.cpisync.application.service.synchronization.model.SynchronizationSnapshot;
import com.figaf.training.cpisync.application.service.synchronization.model.SynchronizationSnapshotMetadata;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.apache.commons.collections4.queue.SynchronizedQueue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SynchronizationJobScheduler {

    @Autowired
    @Qualifier("synchronizationJobExecutor")
    private ThreadPoolTaskExecutor jobExecutor;

    private final Queue<AbstractSynchronizationJob> jobs = SynchronizedQueue.synchronizedQueue(new CircularFifoQueue<>(100));

    public CompletableFuture<UUID> startSynchronization(AbstractSynchronizationJob job) {
        return CompletableFuture.supplyAsync(() -> {
                jobs.add(job);
                job.run();
                return job.getId();
            }, jobExecutor)
            .exceptionally(throwable -> {
                job.markFailed(throwable);
                return job.getId();
            });
    }

    public boolean hasRunningJobs() {
        return jobExecutor.getThreadPoolExecutor().getActiveCount() > 0;
    }

    public Optional<SynchronizationSnapshot> getProgress(UUID jobId) {
        Optional<AbstractSynchronizationJob> runningJob = jobs.stream()
            .filter(j -> Objects.equals(j.getId(), jobId))
            .findFirst();
        return runningJob.map(AbstractSynchronizationJob::getFullSnapshot).or(Optional::empty);
    }

    public List<SynchronizationSnapshotMetadata> getAllJobsMetadata() {
        List<SynchronizationSnapshotMetadata> metadata = new LinkedList<>();
        jobs.forEach(snapshot -> metadata.add(snapshot.getMetadataSnapshot()));
        return metadata;
    }

    public void clearAll() {
        jobExecutor.getThreadPoolExecutor().purge();
        jobs.clear();
    }

}


