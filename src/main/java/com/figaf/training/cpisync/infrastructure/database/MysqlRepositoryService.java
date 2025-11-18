package com.figaf.training.cpisync.infrastructure.database;

import com.figaf.training.cpisync.application.dto.SyncedObjectVersionPayload;
import com.figaf.training.cpisync.application.dto.SynchronizationActionType;
import com.figaf.training.cpisync.domain.RepositoryService;
import com.figaf.training.cpisync.domain.SyncedObjectHistory;
import com.figaf.training.cpisync.domain.SyncedObjectType;
import com.figaf.training.cpisync.domain.SyncedObjectVersion;
import com.figaf.training.cpisync.infrastructure.SynchronizationMapper;
import com.figaf.training.cpisync.infrastructure.database.entities.SyncedObjectHistoryEntity;
import com.figaf.training.cpisync.infrastructure.database.entities.SyncedObjectVersionEntity;
import com.figaf.training.cpisync.infrastructure.database.repository.MysqlSyncedObjectRepository;
import com.figaf.training.cpisync.infrastructure.database.repository.SyncedObjectVersionRepository;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.repository.type", havingValue = "mysql")
public class MysqlRepositoryService implements RepositoryService {

    private final SynchronizationMapper mapper;
    private final MysqlSyncedObjectRepository historyRepository;
    private final SyncedObjectVersionRepository versionRepository;

    //TODO clear separation according DDD, to work only with entities on infrastructure level
    //TODO: do not translate the objects with mapper from dto to entity
    @Override
    @Transactional
    public Optional<SynchronizationRepositoryChange> appendVersionIfNecessary(
        String technicalName,
        SyncedObjectType type,
        Instant remoteModificationDate,
        String remoteVersion,
        Supplier<SyncedObjectVersionPayload> payloadSupplier
    ) {
        SyncedObjectHistoryEntity history = ensureHistory(technicalName, type);
        Optional<SyncedObjectVersion> latest = queryLatestVersion(history.getId());
        if (latest.isEmpty() || latest.get().isDeleted()) {
            SyncedObjectVersion created = persistNewVersion(history, payloadSupplier.get(), latest, false);
            return Optional.of(new SynchronizationRepositoryChange(SynchronizationActionType.REGISTERED, created));
        }

        SyncedObjectVersion current = latest.get();
        if (hasRemoteChanges(remoteModificationDate, remoteVersion, current)) {
            SyncedObjectVersion updated = persistNewVersion(history, payloadSupplier.get(), latest, false);
            return Optional.of(new SynchronizationRepositoryChange(SynchronizationActionType.UPDATED, updated));
        }
        return Optional.empty();
    }

    @Override
    @Transactional
    public Optional<SyncedObjectVersion> markDeleted(String technicalName, SyncedObjectType type) {
        SyncedObjectHistoryEntity history = findHistoryEntity(technicalName, type);
        if (history == null) {
            return Optional.empty();
        }

        Optional<SyncedObjectVersion> latest = queryLatestVersion(history.getId());
        if (latest.isEmpty()) {
            return Optional.empty();
        }

        SyncedObjectVersion current = latest.get();
        if (current.isDeleted()) {
            return Optional.empty();
        }

        SyncedObjectVersionPayload payload = mapper.toPayloadFromExisting(current, false);
        SyncedObjectVersion deletedVersion = persistNewVersion(history, payload, latest, true);
        return Optional.of(deletedVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<SyncedObjectHistory> findAllByType(SyncedObjectType type) {
        List<SyncedObjectVersionEntity> latestVersions = versionRepository.findLatestVersionsByType(type.name());
        Map<Long, SyncedObjectHistory> histories = new LinkedHashMap<>();
        for (SyncedObjectVersionEntity versionEntity : latestVersions) {
            SyncedObjectHistoryEntity historyEntity = versionEntity.getHistory();
            SyncedObjectHistory history = histories.computeIfAbsent(
                historyEntity.getId(),
                ignored -> new SyncedObjectHistory(historyEntity.getTechnicalName(), type)
            );
            history.addVersion(mapper.toDomain(versionEntity, false));
        }
        return histories.values();
    }

    @Override
    @Transactional
    public void deleteAll() {
        historyRepository.deleteAll();
    }

    private SyncedObjectVersion persistNewVersion(
        SyncedObjectHistoryEntity history,
        SyncedObjectVersionPayload payload,
        Optional<SyncedObjectVersion> latest,
        boolean deleted
    ) {
        int nextVersionNumber = latest.map(SyncedObjectVersion::getVersionNumber).orElse(0) + 1;
        SyncedObjectVersion version = buildVersionFromPayload(payload, nextVersionNumber, deleted);
        SyncedObjectVersionEntity entity = mapper.toEntity(history, version);
        Instant validityStart = version.getSynchronizedAt();
        entity.setValidFrom(validityStart);
        entity.setValidTo(null);
        if (nextVersionNumber > 1) {
            versionRepository.closeVersion(history.getId(), nextVersionNumber - 1, validityStart);
        }
        versionRepository.save(entity);
        return version;
    }

    private SyncedObjectVersion buildVersionFromPayload(SyncedObjectVersionPayload payload, int versionNumber, boolean deleted) {
        Instant synchronizedAt = Instant.now();
        SyncedObjectVersion.Builder builder = SyncedObjectVersion.builder()
            .versionNumber(versionNumber)
            .deleted(deleted)
            .synchronizedAt(synchronizedAt);
        payload.applyTo(builder);
        return builder.build();
    }

    private Optional<SyncedObjectVersion> queryLatestVersion(long historyId) {
        return historyRepository.findLatestVersionByHistoryId(historyId, PageRequest.of(0, 1))
            .stream()
            .findFirst()
            .map(entity -> mapper.toDomain(entity, false));
    }

    private SyncedObjectHistoryEntity ensureHistory(String technicalName, SyncedObjectType type) {
        SyncedObjectHistoryEntity existing = findHistoryEntity(technicalName, type);
        if (existing != null) {
            return existing;
        }

        SyncedObjectHistoryEntity history = new SyncedObjectHistoryEntity();
        history.setTechnicalName(technicalName);
        history.setObjectType(type.name());
        return historyRepository.save(history);
    }

    private SyncedObjectHistoryEntity findHistoryEntity(String technicalName, SyncedObjectType type) {
        return historyRepository
            .findByTechnicalNameAndObjectType(technicalName, type.name())
            .orElse(null);
    }

    private boolean hasRemoteChanges(Instant remoteModification, String remoteVersion, SyncedObjectVersion currentVersion) {
        if (remoteModification != null) {
            Optional<Instant> currentModification = currentVersion.getModificationDate();
            if (currentModification.isEmpty()
                || remoteModification.getEpochSecond() > currentModification.get().getEpochSecond()) {
                return true;
            }
        }
        String currentRemoteVersion = currentVersion.getRemoteVersion().orElse(null);
        return !Objects.equals(remoteVersion, currentRemoteVersion);
    }
}
