package com.figaf.training.cpisync.infrastructure.controller;

import com.figaf.training.cpisync.application.dto.SyncedObjectLatestVersionResponse;
import com.figaf.training.cpisync.application.dto.SynchronizationRequest;
import com.figaf.training.cpisync.application.service.SyncedObjectsService;
import com.figaf.training.cpisync.application.service.synchronization.SynchronizationJobFactory;
import com.figaf.training.cpisync.application.service.synchronization.SynchronizationJobScheduler;
import com.figaf.training.cpisync.application.service.synchronization.model.AbstractSynchronizationJob;
import com.figaf.training.cpisync.application.service.synchronization.model.SynchronizationSnapshot;
import com.figaf.training.cpisync.application.service.synchronization.model.SynchronizationSnapshotMetadata;
import com.figaf.training.cpisync.domain.SyncedObjectType;
import com.figaf.training.cpisync.infrastructure.SynchronizationMapper;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/synchronizations")
@RequiredArgsConstructor
public class SynchronizationController {

    private final ReentrantLock SYNC_LOCK = new ReentrantLock(false);
    private final SyncedObjectsService syncedObjectsService;
    private final SynchronizationJobScheduler scheduler;
    private final SynchronizationMapper mapper;
    private final SynchronizationJobFactory jobFactory;

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ResponseEntity<?>> synchronize(@RequestBody(required = false) SynchronizationRequest request) {
        log.info("POST /synchronizations");
        if (SYNC_LOCK.tryLock()) {
            Set<String> packageFilters = normalizePackageFilters(request);
            AbstractSynchronizationJob job = packageFilters == null
                ? jobFactory.createDefaultJob()
                : jobFactory.createJob(packageFilters);
            return scheduler.startSynchronization(job).thenApply(ResponseEntity::ok);
        } else {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body("Synchronization was already started and is in progress..."));
        }
    }

    @GetMapping(value = "/jobs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<SynchronizationSnapshotMetadata>> getAllJobs() {
        log.info("GET /synchronizations/jobs");
        return ResponseEntity.ok(scheduler.getAllJobsMetadata());
    }

    @GetMapping(value = "/{jobId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SynchronizationSnapshot> getSynchronizationProgress(@PathVariable UUID jobId) {
        log.info("GET /synchronizations/{}", jobId);
        return scheduler
            .getProgress(jobId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping(value = "/latest-objects", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SyncedObjectLatestVersionResponse> getLatestSyncedObjects(
        @RequestParam(value = "type") SyncedObjectType type
    ) {
        log.info("GET /latest-objects");
        return syncedObjectsService.latestVersionsForType(type)
            .entrySet()
            .stream()
            .map(entry -> mapper.toResponse(entry.getKey(), entry.getValue()))
            .sorted(Comparator.comparing(SyncedObjectLatestVersionResponse::synchronizedAt).reversed())
            .toList();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearAllSyncedObjects() {
        log.warn("DELETE /synchronizations (clear all data)");
        syncedObjectsService.clearAll();
        scheduler.clearAll();
        return ResponseEntity.noContent().build();
    }

    private Set<String> normalizePackageFilters(SynchronizationRequest request) {
        if (request == null || request.packageTechnicalNames() == null || request.packageTechnicalNames().isEmpty()) {
            return null;
        }
        Set<String> result = request.packageTechnicalNames().stream()
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        return result.isEmpty() ? null : result;
    }

}
