package com.figaf.training.cpisync.domain;

import com.figaf.training.cpisync.infrastructure.database.SynchronizationRepositoryChange;
import com.figaf.training.cpisync.application.dto.SyncedObjectVersionPayload;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Defines the persistence contract for storing and retrieving synchronized CPI objects and their versions.
 */
public interface RepositoryService {

    /**
     * Marks a synchronized object as deleted by appending a tombstone version, if the history exists.
     *
     * @param technicalName CPI object's technical name
     * @param type object type (package, flow, etc.)
     * @return the deleted version if a new one was created, otherwise empty
     */
    Optional<SyncedObjectVersion> markDeleted(String technicalName, SyncedObjectType type);

    /**
     * Retrieves all histories that belong to the specified object type.
     *
     * @param type object category used to scope the lookup
     * @return collection of histories with their cached versions
     */
    Collection<SyncedObjectHistory> findAllByType(SyncedObjectType type);

    /**
     * Appends a new version when remote metadata differs from the latest stored version.
     *
     * @param technicalName CPI object's technical name
     * @param type object type (package, flow, etc.)
     * @param remoteModificationDate last modification timestamp reported by CPI
     * @param remoteVersion remote system version identifier
     * @param payloadSupplier lazy supplier that builds the payload if a new version is needed
     * @return description of the repository change or empty if nothing changed
     */
    Optional<SynchronizationRepositoryChange> appendVersionIfNecessary(
        String technicalName,
        SyncedObjectType type,
        Instant remoteModificationDate,
        String remoteVersion,
        Supplier<SyncedObjectVersionPayload> payloadSupplier
    );

    /**
     * Removes every stored history and version (used for test reset / admin cleanup).
     */
    void deleteAll();
}
