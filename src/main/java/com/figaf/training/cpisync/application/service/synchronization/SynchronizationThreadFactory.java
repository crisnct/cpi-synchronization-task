package com.figaf.training.cpisync.application.service.synchronization;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class SynchronizationThreadFactory implements ThreadFactory {

    private final String threadNamePrefix;
    private final boolean daemon;
    private final AtomicLong threadCounter = new AtomicLong(0);

    private SynchronizationThreadFactory(String threadNamePrefix, boolean daemon) {
        this.threadNamePrefix = threadNamePrefix;
        this.daemon = daemon;
    }

    public static ThreadFactory nonDaemon(String threadNamePrefix) {
        return new SynchronizationThreadFactory(threadNamePrefix, false);
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setName(threadNamePrefix + getAndIncrementWithReset());
        thread.setDaemon(daemon);
        return thread;
    }

    private long getAndIncrementWithReset() {
        return threadCounter.getAndUpdate(current -> current + 1);
    }
}
