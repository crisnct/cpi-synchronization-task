package com.figaf.training.cpisync.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.figaf.integration.cpi.client.CpiRuntimeArtifactClient;
import com.figaf.integration.cpi.client.IntegrationPackageClient;
import com.figaf.integration.cpi.entity.designtime_artifacts.CpiArtifact;
import com.figaf.integration.cpi.entity.designtime_artifacts.CpiArtifactType;
import com.figaf.integration.cpi.entity.designtime_artifacts.IntegrationPackage;
import com.figaf.training.cpisync.application.service.SyncedObjectsService;
import com.figaf.training.cpisync.application.service.synchronization.SynchronizationJobFactory;
import com.figaf.training.cpisync.application.service.synchronization.SynchronizationJobScheduler;
import com.figaf.training.cpisync.application.service.synchronization.SynchronizationService;
import com.figaf.training.cpisync.application.service.synchronization.model.AbstractSynchronizationJob;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.config.location=classpath:/application-test.yml")
@Tag("integration")
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Import(SynchronizationIntegrationTest.TestOverrides.class)
public class SynchronizationIntegrationTest {

    @Autowired
    private SynchronizationJobFactory jobFactory;

    @Autowired
    private SynchronizationJobScheduler scheduler;

    @Autowired
    private SyncedObjectsService syncedObjectsService;

    @Autowired
    private SynchronizationService synchronizationService;

    @Autowired
    private ObjectMapper objectMapper;

    //MOCKS ---------------------------------
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IntegrationPackageClient integrationPackageClient;

    @Autowired
    private CpiRuntimeArtifactClient cpiRuntimeArtifactClient;
    //---------------------------------------

    @BeforeEach
    void resetMocks() {
        reset(integrationPackageClient, cpiRuntimeArtifactClient);
    }

    @SuppressWarnings("BusyWait")
    @Test
    void registeredCountsRemainConsistentAfterConcurrentJobs() throws Exception {
        int jobCount = 3;

        final List<IntegrationPackage> packages = new ArrayList<>();
        for (int i = 0; i < 500; i++) {
            IntegrationPackage pkg = new IntegrationPackage();
            pkg.setTechnicalName("pkg" + i);
            pkg.setExternalId(UUID.randomUUID().toString());
            pkg.setVersion("1.0.0");
            packages.add(pkg);
        }

        final List<CpiArtifact> artifacts = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            CpiArtifact artifact = new CpiArtifact();
            artifact.setTechnicalName("artifact" + i);
            artifact.setExternalId(UUID.randomUUID().toString());
            artifacts.add(artifact);
        }

        when(integrationPackageClient.getIntegrationPackages(any(), any())).thenReturn(packages);
        when(cpiRuntimeArtifactClient.getArtifactsByPackage(any(), eq("pkg1"), any(), any(), eq(CpiArtifactType.IFLOW)))
            .thenReturn(artifacts);

        final List<AbstractSynchronizationJob> jobs = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            AbstractSynchronizationJob job = jobFactory.createDefaultJob();
            scheduler.startSynchronization(job);
            jobs.add(job);
        }

        //Wait until all jobs are done
        while (scheduler.hasRunningJobs()) {
            Thread.sleep(100);
        }

        long sum = 0;
        for (AbstractSynchronizationJob job : jobs) {
            sum += job.getFullSnapshot().metadata().registeredCount();
        }

        assertEquals(sum, packages.size() + artifacts.size(), "Concurrent jobs process same packages");
    }

    @TestConfiguration
    static class TestOverrides {

        @Bean
        @Primary
        IntegrationPackageClient mockJobFactory() {
            return mock(IntegrationPackageClient.class);
        }

        @Bean
        @Primary
        CpiRuntimeArtifactClient mockSyncedObjectsService() {
            return mock(CpiRuntimeArtifactClient.class);
        }
    }
}
