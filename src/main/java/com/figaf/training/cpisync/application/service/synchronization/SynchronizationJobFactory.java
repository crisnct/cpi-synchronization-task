package com.figaf.training.cpisync.application.service.synchronization;

import com.figaf.training.cpisync.application.service.synchronization.model.AbstractSynchronizationJob;
import com.figaf.training.cpisync.application.service.synchronization.model.DefaultSynchronizationJob;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SynchronizationJobFactory {

    private final SynchronizationService synchronizationService;

    public AbstractSynchronizationJob createDefaultJob() {
        return new DefaultSynchronizationJob(synchronizationService, null);
    }

    public AbstractSynchronizationJob createJob(Set<String> packageTechnicalNames) {
        Set<String> sanitized = packageTechnicalNames == null || packageTechnicalNames.isEmpty()
            ? null
            : Set.copyOf(packageTechnicalNames);
        return new DefaultSynchronizationJob(synchronizationService, sanitized);
    }
}
