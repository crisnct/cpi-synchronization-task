package com.figaf.training.cpisync.application.service.synchronization.model;

import com.figaf.training.cpisync.application.service.synchronization.SynchronizationService;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DefaultSynchronizationJob extends AbstractSynchronizationJob {

    private final SynchronizationService synchronizationService;
    private final Set<String> packageTechnicalNames;

    @Override
    public void run() {
        try {
            synchronizationService.runSynchronization(this, packageTechnicalNames);
        } catch (Exception ex) {
            if (isInterruptSignal(ex)) {
                Thread.currentThread().interrupt();
                log.warn("Synchronization job {} interrupted", this.getId(), ex);
            } else {
                log.error("Synchronization job {} failed", this.getId(), ex);
            }
            if (this.getStatus() != SynchronizationJobStatus.FAILED) {
                this.markFailed(ex);
            }
            //noinspection ConstantValue
            throw (ex instanceof RuntimeException) ? (RuntimeException) ex : new IllegalStateException("Synchronization job failed", ex);
        }
    }

    private boolean isInterruptSignal(Exception ex) {
        return ex instanceof InterruptedException
            || (ex.getCause() instanceof InterruptedException)
            || Thread.currentThread().isInterrupted();
    }

}
