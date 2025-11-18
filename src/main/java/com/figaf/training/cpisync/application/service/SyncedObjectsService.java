package com.figaf.training.cpisync.application.service;

import com.figaf.training.cpisync.domain.SyncedObjectHistory;
import com.figaf.training.cpisync.domain.SyncedObjectType;
import com.figaf.training.cpisync.domain.SyncedObjectVersion;
import com.figaf.training.cpisync.domain.RepositoryService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncedObjectsService {

    private final RepositoryService repository;

    public Map<SyncedObjectDescriptor, SyncedObjectVersion> latestVersionsForType(SyncedObjectType type) {
        final Map<SyncedObjectDescriptor, SyncedObjectVersion> mapLatest = new HashMap<>();
        for (SyncedObjectHistory history : repository.findAllByType(type)) {
            history.getLatestVersion().ifPresent(value ->
                mapLatest.put(new SyncedObjectDescriptor(history.getTechnicalName(), history.getType()), value)
            );
        }
        return mapLatest;
    }

    public void clearAll() {
        log.warn("Removing all synced object data");
        repository.deleteAll();
    }

}
