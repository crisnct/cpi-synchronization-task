package com.figaf.training.cpisync.application.service.synchronization;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SynchronizationConfig {

    @Bean(name = "synchronizationJobExecutor")
    public ThreadPoolExecutor synchronizationJobExecutor(
        @Value("${app.sync.jobExecutorConcurrency:1}") String jobExecutorConcurrency
    ) {
        int concurrentThreads = parsePositive(jobExecutorConcurrency, 1);
        return (ThreadPoolExecutor)Executors.newFixedThreadPool(concurrentThreads, Executors.defaultThreadFactory());
    }

    @Bean(name = "integrationFlowJobExecutor")
    public ThreadPoolExecutor integrationFlowJobExecutor(
        //Maximum number of integration flows that may be synchronized concurrently per job.
        @Value("${app.sync.flowConcurrency:1}")
        String flowConcurrency
    ) {
        int concurrentThreads = parsePositive(flowConcurrency, 1);
        return (ThreadPoolExecutor)Executors.newFixedThreadPool(concurrentThreads, Executors.defaultThreadFactory());
    }

    @Bean(name = "integrationPackagesJobExecutor")
    public ThreadPoolExecutor integrationPackagesJobExecutor(
        //Maximum number of packages that may be synchronized concurrently per job.
        @Value("${app.sync.packagesConcurrency:1}")
        String packagesConcurrency
    ) {
        int concurrentThreads = parsePositive(packagesConcurrency, 1);
        return (ThreadPoolExecutor)Executors.newFixedThreadPool(concurrentThreads, Executors.defaultThreadFactory());
    }

    private int parsePositive(String value, int defaultValue) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
