package com.figaf.training.cpisync.application.service;

import com.figaf.training.cpisync.application.dto.SyncedObjectVersionPayload;
import com.figaf.training.cpisync.application.dto.SynchronizationActionType;
import com.figaf.training.cpisync.domain.RepositoryService;
import com.figaf.training.cpisync.infrastructure.database.SynchronizationRepositoryChange;
import com.figaf.training.cpisync.infrastructure.SynchronizationMapper;
import com.figaf.training.cpisync.domain.SyncedObjectHistory;
import com.figaf.training.cpisync.domain.SyncedObjectType;
import com.figaf.training.cpisync.domain.SyncedObjectVersion;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.repository.type", havingValue = "memory", matchIfMissing = true)
public class InMemoryRepositoryService implements RepositoryService {

    private final SynchronizationMapper mapper;

    private final Map<SyncedObjectType, Map<String, SyncedObjectHistory>> storage = new ConcurrentHashMap<>();

    @Override
    public Optional<SynchronizationRepositoryChange> appendVersionIfNecessary(
        String technicalName,
        SyncedObjectType type,
        Instant remoteModificationDate,
        String remoteVersion,
        Supplier<SyncedObjectVersionPayload> payloadSupplier
    ) {
        SyncedObjectHistory history = storage
            .computeIfAbsent(type, ignored -> new ConcurrentHashMap<>())
            .computeIfAbsent(technicalName, name -> new SyncedObjectHistory(name, type));

        synchronized (history) {
            Optional<SyncedObjectVersion> latest = history.getLatestVersion();

            if (latest.isEmpty() || latest.get().isDeleted()) {
                SyncedObjectVersion createdVersion = addVersion(history, payloadSupplier.get(), false);
                return Optional.of(new SynchronizationRepositoryChange(SynchronizationActionType.REGISTERED, createdVersion));
            }

            SyncedObjectVersion currentVersion = latest.get();
            if (hasRemoteChanges(remoteModificationDate, remoteVersion, currentVersion)) {
                SyncedObjectVersion updatedVersion = addVersion(history, payloadSupplier.get(), false);
                return Optional.of(new SynchronizationRepositoryChange(SynchronizationActionType.UPDATED, updatedVersion));
            }

            return Optional.empty();
        }
    }

    @Override
    public Optional<SyncedObjectVersion> markDeleted(String technicalName, SyncedObjectType type) {
        SyncedObjectHistory history = Optional.ofNullable(storage.get(type))
            .map(map -> map.get(technicalName))
            .orElse(null);

        if (history == null) {
            return Optional.empty();
        }

        synchronized (history) {
            Optional<SyncedObjectVersion> latest = history.getLatestVersion();
            if (latest.isEmpty()) {
                return Optional.empty();
            }
            SyncedObjectVersion current = latest.get();
            if (current.isDeleted()) {
                return Optional.empty();
            }
            SyncedObjectVersion deletedVersion = addVersion(history, mapper.toPayloadFromExisting(current, false), true);
            return Optional.of(deletedVersion);
        }
    }

    @Override
    public Collection<SyncedObjectHistory> findAllByType(SyncedObjectType type) {
        return Optional.ofNullable(storage.get(type))
            .map(map -> map.values().stream().toList())
            .orElseGet(java.util.List::of);
    }

    @Override
    public void deleteAll() {
        storage.clear();
    }

    private SyncedObjectVersion addVersion(SyncedObjectHistory history, SyncedObjectVersionPayload payload, boolean deleted) {
        SyncedObjectVersion version = buildVersion(history, payload, deleted);
        history.addVersion(version);
        return version;
    }

    private SyncedObjectVersion buildVersion(SyncedObjectHistory history, SyncedObjectVersionPayload payload, boolean deleted) {
        int versionNumber = history.getLatestVersion()
            .map(SyncedObjectVersion::getVersionNumber)
            .orElse(0) + 1;

        SyncedObjectVersion.Builder builder = SyncedObjectVersion.builder()
            .versionNumber(versionNumber)
            .deleted(deleted)
            .synchronizedAt(Instant.now());

        payload.applyTo(builder);

        return builder.build();
    }

    private boolean hasRemoteChanges(Instant remoteModification, String remoteVersion, SyncedObjectVersion currentVersion) {
        if (isRemoteNewer(remoteModification, currentVersion.getModificationDate().orElse(null))) {
            return true;
        }
        String currentRemoteVersion = currentVersion.getRemoteVersion().orElse(null);
        return !Objects.equals(remoteVersion, currentRemoteVersion);
    }

    private boolean isRemoteNewer(Instant remoteModification, Instant currentModification) {
        if (remoteModification == null) {
            return false;
        }
        if (currentModification == null) {
            return true;
        }
        return remoteModification.isAfter(currentModification);
    }
}
