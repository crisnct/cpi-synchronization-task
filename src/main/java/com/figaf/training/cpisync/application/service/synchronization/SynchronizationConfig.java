package com.figaf.training.cpisync.application.service.synchronization;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class SynchronizationConfig {

    @Bean(name = "synchronizationJobExecutor")
    public ThreadPoolTaskExecutor synchronizationJobExecutor(
        @Value("${app.sync.jobExecutorConcurrency:1}") String jobExecutorConcurrency,
        @Value("${app.sync.jobQueueCapacity:10}") String jobQueueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("sync-job-");
        int poolSize = parsePositive(jobExecutorConcurrency, 1);
        int queueCapacity = parsePositive(jobQueueCapacity, poolSize);
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }

    @Bean(name = "integrationFlowJobExecutor")
    public ExecutorService integrationFlowJobExecutor(
        //Maximum number of integration flows that may be synchronized concurrently per job.
        @Value("${app.sync.flowConcurrency:1}")
        String flowConcurrency
    ) {
        int concurrentThreads = parsePositive(flowConcurrency, 1);
        return Executors.newFixedThreadPool(concurrentThreads, SynchronizationThreadFactory.nonDaemon("sync-flow-"));
    }

    @Bean(name = "integrationPackagesJobExecutor")
    public ExecutorService integrationPackagesJobExecutor(
        //Maximum number of packages that may be synchronized concurrently per job.
        @Value("${app.sync.packagesConcurrency:1}")
        String packagesConcurrency
    ) {
        int concurrentThreads = parsePositive(packagesConcurrency, 1);
        return Executors.newFixedThreadPool(concurrentThreads, SynchronizationThreadFactory.nonDaemon("sync-package-"));
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
