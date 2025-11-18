package com.figaf.training.cpisync.infrastructure.database.repository;

import com.figaf.training.cpisync.infrastructure.database.entities.SyncedObjectVersionEntity;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SyncedObjectVersionRepository extends JpaRepository<SyncedObjectVersionEntity, Long> {

    @Query(
        """
            SELECT v
            FROM SyncedObjectVersionEntity v
            JOIN FETCH v.history h
            WHERE h.objectType = :type AND v.validTo IS NULL
            ORDER BY h.technicalName
            """
    )
    List<SyncedObjectVersionEntity> findLatestVersionsByType(@Param("type") String type);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
            UPDATE SyncedObjectVersionEntity v
            SET v.validTo = :validTo
            WHERE v.history.id = :historyId
              AND v.versionNumber = :versionNumber
              AND v.validTo IS NULL
            """
    )
    void closeVersion(
        @Param("historyId") long historyId,
        @Param("versionNumber") int versionNumber,
        @Param("validTo") Instant validTo
    );
}
