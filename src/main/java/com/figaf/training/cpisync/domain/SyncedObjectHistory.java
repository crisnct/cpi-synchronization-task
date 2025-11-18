package com.figaf.training.cpisync.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.Getter;

public class SyncedObjectHistory {

    /** Example: PKG_ORDER_PROC */
    @Getter
    private final String technicalName;

    @Getter
    private final SyncedObjectType type;

    private final List<SyncedObjectVersion> versions = new ArrayList<>();

    public SyncedObjectHistory(String technicalName, SyncedObjectType type) {
        this.technicalName = technicalName;
        this.type = type;
    }

    public synchronized SyncedObjectVersion addVersion(SyncedObjectVersion version) {
        versions.add(version);
        return version;
    }

    public synchronized Optional<SyncedObjectVersion> getLatestVersion() {
        if (versions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(versions.get(versions.size() - 1));
    }

    public synchronized List<SyncedObjectVersion> getVersions() {
        return Collections.unmodifiableList(new ArrayList<>(versions));
    }

}

