package com.figaf.training.cpisync.application.service.synchronization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.figaf.integration.common.entity.RequestContext;
import com.figaf.integration.cpi.client.CpiRuntimeArtifactClient;
import com.figaf.integration.cpi.client.IntegrationPackageClient;
import com.figaf.integration.cpi.entity.designtime_artifacts.CpiArtifact;
import com.figaf.integration.cpi.entity.designtime_artifacts.CpiArtifactType;
import com.figaf.integration.cpi.entity.designtime_artifacts.IntegrationPackage;
import com.figaf.training.cpisync.application.dto.SyncedObjectVersionPayload;
import com.figaf.training.cpisync.application.dto.SynchronizationActionType;
import com.figaf.training.cpisync.application.dto.SynchronizationResultEntry;
import com.figaf.training.cpisync.application.service.synchronization.model.SynchronizationJobProgressTracker;
import com.figaf.training.cpisync.domain.RepositoryService;
import com.figaf.training.cpisync.domain.SyncedObjectHistory;
import com.figaf.training.cpisync.domain.SyncedObjectType;
import com.figaf.training.cpisync.domain.SyncedObjectVersion;
import com.figaf.training.cpisync.infrastructure.SynchronizationMapper;
import com.figaf.training.cpisync.infrastructure.cpi.CpiSystemConnectionParameters;
import com.figaf.training.cpisync.infrastructure.cpi.RequestContextFactory;
import jakarta.annotation.Resource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SynchronizationService {

    private final IntegrationPackageClient integrationPackageClient;
    private final CpiRuntimeArtifactClient cpiRuntimeArtifactClient;
    private final CpiSystemConnectionParameters connectionParameters;
    private final RepositoryService repository;
    private final SynchronizationMapper mapper;

    @Resource(name = "integrationFlowJobExecutor")
    private ExecutorService flowsJobExecutor;

    @Resource(name = "integrationPackagesJobExecutor")
    private ExecutorService packagesJobExecutor;

    public void runSynchronization(
        SynchronizationJobProgressTracker progressTracker,
        Collection<String> packageTechnicalNames
    ) {
        Set<String> packageFilter = normalizePackageFilter(packageTechnicalNames);
        progressTracker.markStarted();
        try {
            RequestContext requestContext = RequestContextFactory.createRequestContextForWebApi(connectionParameters);
            List<IntegrationPackage> packages = integrationPackageClient.getIntegrationPackages(requestContext, null);
            List<IntegrationPackage> scopedPackages = this.filterPackages(packages, packageFilter);

            Set<String> remotePackages = this.synchronizePackagesAsync(scopedPackages, progressTracker);
            Set<String> remoteFlows = this.synchronizeFlowsAsync(scopedPackages, progressTracker);

            this.markDeletedMissing(remotePackages, SyncedObjectType.INTEGRATION_PACKAGE, progressTracker, packageFilter);
            this.markDeletedMissing(remoteFlows, SyncedObjectType.INTEGRATION_FLOW, progressTracker, packageFilter);

            progressTracker.markCompleted();
        } catch (Exception ex) {
            progressTracker.markFailed(ex);
            throw ex;
        }
    }

    private List<IntegrationPackage> filterPackages(List<IntegrationPackage> packages, Set<String> packageFilter) {
        if (packageFilter == null || packageFilter.isEmpty()) {
            return packages;
        }
        return packages.stream()
            .filter(pkg -> pkg.getTechnicalName() != null && packageFilter.contains(pkg.getTechnicalName()))
            .toList();
    }

    private Set<String> normalizePackageFilter(Collection<String> packageTechnicalNames) {
        if (packageTechnicalNames == null || packageTechnicalNames.isEmpty()) {
            return null;
        }
        Set<String> normalized = packageTechnicalNames.stream()
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        return normalized.isEmpty() ? null : normalized;
    }

    private Optional<String> synchronizePackagesSync(IntegrationPackage pkg, SynchronizationJobProgressTracker progressTracker) {
        String technicalName = pkg.getTechnicalName();
        if (StringUtils.isEmpty(technicalName)) {
            return Optional.empty();
        }
        Instant remoteModification = this.toInstant(pkg.getModificationDate());
        try {
            this.handleRemoteObject(
                technicalName,
                SyncedObjectType.INTEGRATION_PACKAGE,
                remoteModification,
                pkg.getVersion(),
                () -> buildIntegrationPackagePayload(pkg),
                progressTracker
            );
        } catch (DataIntegrityViolationException ex) {
            log.warn("Package {} was already updated on other thread", technicalName);
        }
        return Optional.of(technicalName);
    }

    private Set<String> synchronizePackagesAsync(
        List<IntegrationPackage> packages,
        SynchronizationJobProgressTracker progressTracker
    ) {
        Set<String> remotePackageNames = new HashSet<>();

        List<Callable<Optional<String>>> tasks = new ArrayList<>();
        for (IntegrationPackage pkg : packages) {
            tasks.add(() -> this.synchronizePackagesSync(pkg, progressTracker));
        }

        try {
            for (Future<Optional<String>> future : packagesJobExecutor.invokeAll(tasks)) {
                try {
                    future.get().ifPresent(remotePackageNames::add);
                } catch (ExecutionException ex) {
                    throw new IllegalStateException("Failed to synchronize integration packages", ex.getCause());
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Integration packages synchronization interrupted", ex);
        }

        return remotePackageNames;
    }

    private Set<String> synchronizeFlowsAsync(
        List<IntegrationPackage> packages,
        SynchronizationJobProgressTracker progressTracker
    ) {
        Set<String> remoteFlows = ConcurrentHashMap.newKeySet();
        try {
            List<Callable<Collection<String>>> tasks = packages.stream()
                .map(pkg -> (Callable<Collection<String>>) () -> synchronizeFlowsForPackage(pkg, progressTracker))
                .toList();
            List<Future<Collection<String>>> futures = flowsJobExecutor.invokeAll(tasks);
            for (Future<Collection<String>> future : futures) {
                Collection<String> flows;
                try {
                    flows = future.get();
                    remoteFlows.addAll(flows);
                } catch (ExecutionException ex) {
                    throw new IllegalStateException("Failed to synchronize integration flows", ex.getCause());
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Integration flows synchronization interrupted", ex);
        }
        return remoteFlows;
    }

    private Collection<String> synchronizeFlowsForPackage(
        IntegrationPackage pkg,
        SynchronizationJobProgressTracker progressTracker
    ) {
        String packageTechName = pkg.getTechnicalName();
        if (packageTechName == null) {
            return Collections.emptySet();
        }
        String packageExternalId = pkg.getExternalId();
        if (packageExternalId == null) {
            throw new IllegalStateException(
                "Package %s is missing externalId, aborting synchronization to avoid data loss".formatted(packageTechName));
        }

        RequestContext requestContext = RequestContextFactory.createRequestContextForWebApi(connectionParameters);
        List<CpiArtifact> artifacts = cpiRuntimeArtifactClient.getArtifactsByPackage(
            requestContext,
            packageTechName,
            pkg.getDisplayedName(),
            packageExternalId,
            CpiArtifactType.IFLOW);

        Set<String> remoteFlows = new HashSet<>();
        for (CpiArtifact artifact : artifacts) {
            String technicalName = artifact.getTechnicalName();
            if (technicalName == null) {
                continue;
            }
            remoteFlows.add(technicalName);

            Instant remoteModification = this.toInstant(artifact.getModificationDate());
            this.handleRemoteObject(
                technicalName,
                SyncedObjectType.INTEGRATION_FLOW,
                remoteModification,
                artifact.getVersion(),
                () -> buildIntegrationFlowPayload(requestContext, pkg, artifact),
                progressTracker
            );

        }
        return remoteFlows;
    }

    private void handleRemoteObject(
        String technicalName,
        SyncedObjectType type,
        Instant remoteModificationDate,
        String remoteVersion,
        Supplier<SyncedObjectVersionPayload> payloadSupplier,
        SynchronizationJobProgressTracker progressTracker
    ) {
        repository.appendVersionIfNecessary(technicalName, type, remoteModificationDate, remoteVersion, payloadSupplier)
            .ifPresent(change -> {
                progressTracker.addEntry(toResultEntry(type, technicalName, change.action(), change.version()));
                if (change.action() == SynchronizationActionType.REGISTERED) {
                    log.info("Registered {} {}", type, technicalName);
                } else if (change.action() == SynchronizationActionType.UPDATED) {
                    log.info("Updated {} {}", type, technicalName);
                }
            });
    }

    private void markDeletedMissing(
        Set<String> remoteKeys,
        SyncedObjectType type,
        SynchronizationJobProgressTracker progressTracker,
        Set<String> packageFilter
    ) {
        for (SyncedObjectHistory history : repository.findAllByType(type)) {
            if (!isWithinScope(history, type, packageFilter)) {
                continue;
            }
            if (!remoteKeys.contains(history.getTechnicalName())) {
                repository.markDeleted(history.getTechnicalName(), type).ifPresent(version -> {
                    progressTracker.addEntry(
                        toResultEntry(type, history.getTechnicalName(), SynchronizationActionType.MARKED_AS_DELETED, version)
                    );
                    log.info("Marked {} {} as deleted", type, history.getTechnicalName());
                });
            }
        }
    }

    private boolean isWithinScope(SyncedObjectHistory history, SyncedObjectType type, Set<String> packageFilter) {
        if (packageFilter == null || packageFilter.isEmpty()) {
            return true;
        }
        return switch (type) {
            case INTEGRATION_PACKAGE -> packageFilter.contains(history.getTechnicalName());
            case INTEGRATION_FLOW -> history.getLatestVersion()
                .flatMap(SyncedObjectVersion::getParentTechnicalName)
                .map(packageFilter::contains)
                .orElse(false);
            default -> true;
        };
    }

    private SynchronizationResultEntry toResultEntry(
        SyncedObjectType type,
        String technicalName,
        SynchronizationActionType action,
        SyncedObjectVersion version
    ) {
        return new SynchronizationResultEntry(
            type,
            technicalName,
            action,
            version.getVersionNumber(),
            version.isDeleted(),
            version.getSynchronizedAt()
        );
    }

    private Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }

    private SyncedObjectVersionPayload buildIntegrationPackagePayload(IntegrationPackage pkg) {
        byte[] payloadBytes;
        try {
            payloadBytes = mapper.serializeToByteArray(pkg);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize integration package %s".formatted(pkg.getTechnicalName()), e);
        }
        return SyncedObjectVersionPayload.builder()
            .modificationDate(toInstant(pkg.getModificationDate()))
            .remoteVersion(pkg.getVersion())
            .displayName(pkg.getDisplayedName())
            .externalId(pkg.getExternalId())
            .creationDate(toInstant(pkg.getCreationDate()))
            .createdBy(pkg.getCreatedBy())
            .modifiedBy(pkg.getModifiedBy())
            .payload(payloadBytes)
            .payloadContentType(MediaType.APPLICATION_JSON_VALUE)
            .payloadFileName(buildPackageFileName(pkg.getTechnicalName()))
            .build();
    }

    private SyncedObjectVersionPayload buildIntegrationFlowPayload(
        RequestContext requestContext,
        IntegrationPackage pkg,
        CpiArtifact artifact
    ) {
        byte[] artifactPayload = downloadIntegrationFlowArchive(requestContext, pkg.getExternalId(), artifact.getExternalId());
        return SyncedObjectVersionPayload.builder()
            .modificationDate(toInstant(artifact.getModificationDate()))
            .remoteVersion(artifact.getVersion())
            .displayName(artifact.getDisplayedName())
            .externalId(artifact.getExternalId())
            .parentTechnicalName(artifact.getPackageTechnicalName())
            .parentExternalId(artifact.getPackageExternalId())
            .creationDate(toInstant(artifact.getCreationDate()))
            .createdBy(artifact.getCreatedBy())
            .modifiedBy(artifact.getModifiedBy())
            .payload(artifactPayload)
            .payloadContentType("application/zip")
            .payloadFileName(buildIntegrationFlowFileName(artifact))
            .build();
    }

    private byte[] downloadIntegrationFlowArchive(RequestContext requestContext, String packageExternalId, String artifactExternalId) {
        if (packageExternalId == null || artifactExternalId == null) {
            throw new IllegalStateException("Cannot download artifact payload without external identifiers");
        }
        try {
            return cpiRuntimeArtifactClient.downloadArtifact(requestContext, packageExternalId, artifactExternalId);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to download artifact payload for %s".formatted(artifactExternalId), ex);
        }
    }

    private String buildPackageFileName(String technicalName) {
        return Optional.ofNullable(technicalName).map(name -> name + ".json").orElse("package.json");
    }

    private String buildIntegrationFlowFileName(CpiArtifact artifact) {
        String baseName = Optional.ofNullable(artifact.getExternalId())
            .filter(StringUtils::isNotBlank)
            .orElse(Optional.ofNullable(artifact.getTechnicalName()).orElse("integration-flow"));
        return baseName + ".zip";
    }

}
