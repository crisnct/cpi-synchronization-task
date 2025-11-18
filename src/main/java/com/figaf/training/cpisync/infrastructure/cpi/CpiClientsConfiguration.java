package com.figaf.training.cpisync.infrastructure.cpi;

import com.figaf.integration.common.factory.HttpClientsFactory;
import com.figaf.integration.cpi.client.CpiRuntimeArtifactClient;
import com.figaf.integration.cpi.client.IntegrationPackageClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CpiClientsConfiguration {

    @Bean
    public HttpClientsFactory httpClientsFactoryForCloudIntegration() {
        return new HttpClientsFactory();
    }

    @Bean
    public IntegrationPackageClient integrationPackageClient(HttpClientsFactory httpClientsFactory) {
        return new IntegrationPackageClient(httpClientsFactory);
    }

    @Bean
    public CpiRuntimeArtifactClient cpiRuntimeArtifactClient(HttpClientsFactory httpClientsFactory) {
        return new CpiRuntimeArtifactClient(httpClientsFactory);
    }

}
