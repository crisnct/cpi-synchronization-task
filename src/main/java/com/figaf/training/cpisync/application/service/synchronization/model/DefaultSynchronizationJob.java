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
        synchronizationService.runSynchronization(this, packageTechnicalNames);
    }

}
