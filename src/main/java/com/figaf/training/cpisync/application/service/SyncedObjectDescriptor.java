package com.figaf.training.cpisync.application.service;

import com.figaf.training.cpisync.domain.SyncedObjectType;

public record SyncedObjectDescriptor(
    String technicalName,
    SyncedObjectType type
) { }

