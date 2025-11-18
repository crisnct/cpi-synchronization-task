package com.figaf.training.cpisync.infrastructure.controller;

import com.figaf.integration.common.entity.RequestContext;
import com.figaf.integration.cpi.client.CpiRuntimeArtifactClient;
import com.figaf.integration.cpi.client.IntegrationPackageClient;
import com.figaf.integration.cpi.entity.designtime_artifacts.CpiArtifact;
import com.figaf.integration.cpi.entity.designtime_artifacts.CpiArtifactType;
import com.figaf.integration.cpi.entity.designtime_artifacts.IntegrationPackage;
import com.figaf.training.cpisync.infrastructure.cpi.CpiSystemConnectionParameters;
import com.figaf.training.cpisync.infrastructure.cpi.RequestContextFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/packages")
public class CpiPackageController {

    private final IntegrationPackageClient integrationPackageClient;
    private final CpiRuntimeArtifactClient cpiRuntimeArtifactClient;
    private final CpiSystemConnectionParameters cpiSystemConnectionParameters;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<IntegrationPackage> getIntegrationPackages() {
        log.info("GET /packages");
        return integrationPackageClient.getIntegrationPackages(
            RequestContextFactory.createRequestContextForWebApi(cpiSystemConnectionParameters),
            null
        );
    }

    @GetMapping(value = "/{packageTechnicalName}/integration-flows", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<CpiArtifact> getIntegrationFlowsByPackage(@PathVariable String packageTechnicalName) {
        log.info("GET /packages/{}/integration-flows", packageTechnicalName);

        RequestContext requestContext = RequestContextFactory.createRequestContextForWebApi(cpiSystemConnectionParameters);
        IntegrationPackage integrationPackage = findPackageOrThrow(requestContext, packageTechnicalName);

        return cpiRuntimeArtifactClient.getArtifactsByPackage(
            requestContext,
            integrationPackage.getTechnicalName(),
            integrationPackage.getDisplayedName(),
            integrationPackage.getExternalId(),
            CpiArtifactType.IFLOW
        );
    }

    @GetMapping(value = "/{packageTechnicalName}/artifacts/{artifactExternalId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public HttpEntity<byte[]> downloadArtifactFromPackage(
        @PathVariable String packageTechnicalName,
        @PathVariable String artifactExternalId
    ) {
        log.info("GET /packages/{}/artifacts/{}", packageTechnicalName, artifactExternalId);

        RequestContext requestContext = RequestContextFactory.createRequestContextForWebApi(cpiSystemConnectionParameters);
        IntegrationPackage integrationPackage = findPackageOrThrow(requestContext, packageTechnicalName);

        HttpHeaders headers = new HttpHeaders();
        String safeFileName = buildSafeFileName(artifactExternalId);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s.zip\"".formatted(safeFileName));

        byte[] artifactPayload = cpiRuntimeArtifactClient.downloadArtifact(
            requestContext,
            integrationPackage.getExternalId(),
            artifactExternalId
        );

        return new HttpEntity<>(artifactPayload, headers);
    }

    private IntegrationPackage findPackageOrThrow(RequestContext requestContext, String packageTechnicalName) {
        return integrationPackageClient.getIntegrationPackages(
                requestContext,
                buildTechnicalNameFilter(packageTechnicalName)
            ).stream()
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Package %s was not found".formatted(packageTechnicalName)));
    }

    private String buildTechnicalNameFilter(String technicalName) {
        String sanitized = technicalName == null ? "" : technicalName.replace("'", "''");
        return "TechnicalName eq '%s'".formatted(sanitized);
    }

    private String buildSafeFileName(String raw) {
        String sanitized = raw == null ? "" : raw.replaceAll("[\\r\\n\"\\\\/]", "_").trim();
        if (sanitized.isEmpty()) {
            sanitized = "artifact";
        }
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100);
        }
        return sanitized;
    }

}
