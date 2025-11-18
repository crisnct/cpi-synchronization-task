package com.figaf.training.cpisync.infrastructure.database.repository;

import com.figaf.training.cpisync.infrastructure.database.entities.SyncedObjectHistoryEntity;
import com.figaf.training.cpisync.infrastructure.database.entities.SyncedObjectVersionEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MysqlSyncedObjectRepository extends JpaRepository<SyncedObjectHistoryEntity, Long> {

    Optional<SyncedObjectHistoryEntity> findByTechnicalNameAndObjectType(String technicalName, String objectType);

    @Query(
        """
            SELECT v
            FROM SyncedObjectVersionEntity v
            WHERE v.history.id = :historyId
            ORDER BY v.versionNumber DESC
            """
    )
    List<SyncedObjectVersionEntity> findLatestVersionByHistoryId(
        @Param("historyId") long historyId,
        Pageable pageable
    );
}
