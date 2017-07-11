/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.service.simulator;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javassist.Modifier;

import org.apache.log4j.Logger;

import com.nuodb.storefront.dal.BaseDao;
import com.nuodb.storefront.model.dto.StorefrontStatsReport;
import com.nuodb.storefront.model.dto.Workload;
import com.nuodb.storefront.model.dto.WorkloadFlow;
import com.nuodb.storefront.model.dto.WorkloadStats;
import com.nuodb.storefront.model.dto.WorkloadStep;
import com.nuodb.storefront.model.dto.WorkloadStepStats;
import com.nuodb.storefront.service.ISimulatorService;
import com.nuodb.storefront.service.IStorefrontService;
import com.nuodb.storefront.util.PerformanceUtil;
import com.nuodb.storefront.util.ToStringComparator;

public class SimulatorService implements ISimulator, ISimulatorService {
    private final Logger logger;
    private ScheduledThreadPoolExecutor threadPool;
    private final IStorefrontService svc;
    private final Map<String, WorkloadStats> workloadStatsMap = new HashMap<String, WorkloadStats>();
    private final Map<String, WorkloadStats> aggregateWorkloadStats = new HashMap<>();
    private final Map<WorkloadStep, AtomicInteger> stepCompletionCounts = new TreeMap<WorkloadStep, AtomicInteger>();

    public SimulatorService(IStorefrontService svc) {
        this.logger = svc.getLogger(getClass());
        this.threadPool = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 25);
        this.svc = svc;

        // Seed workload map with predefined workloads
        for (Field field : Workload.class.getFields()) {
            if (Modifier.isStatic(field.getModifiers()) && field.getType().equals(Workload.class)) {
                try {
                    Workload workload = (Workload) field.get(null);
                    getOrCreateWorkloadStats(workload);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        // Seed steps map
        try {
            for (WorkloadStep step : WorkloadStep.values()) {
                if (step.getClass().getField(step.name()).getAnnotation(WorkloadFlow.class) == null) {
                    stepCompletionCounts.put(step, new AtomicInteger(0));
                }
            }
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public Workload getWorkload(String name) {
        synchronized (workloadStatsMap) {
            WorkloadStats stats = workloadStatsMap.get(name);
            return (stats == null) ? null : stats.getWorkload();
        }
    }

    public void addWorkers(Workload workload, int numWorkers, long entryDelayMs) {
        addWorker(new SimulatedUserFactory(this, workload, numWorkers, entryDelayMs), 0);
    }

    public WorkloadStats adjustWorkers(Workload workload, int minActiveWorkers, Integer activeWorkerLimit) {
        logger.info("Adjusting " + workload.getName() + " to " + minActiveWorkers);

        if (activeWorkerLimit != null) {
            if (minActiveWorkers < 0) {
                throw new IllegalArgumentException("minActiveWorkers");
            }
            if (activeWorkerLimit < 0) {
                throw new IllegalArgumentException("activeWorkerLimit");
            }
            if (minActiveWorkers > activeWorkerLimit) {
                throw new IllegalArgumentException("minActiveWorkers cannot exceed activeWorkerLimit");
            }
        }
        if (workload.getMaxWorkers() > 0) {
            if (minActiveWorkers > workload.getMaxWorkers()) {
                throw new IllegalArgumentException("minActiveWorkers cannot exceed workload limit of " + workload.getMaxWorkers());
            }
            if (activeWorkerLimit != null && activeWorkerLimit.intValue() > workload.getMaxWorkers()) {
                throw new IllegalArgumentException("activeWorkerLimit cannot exceed workload limit of " + workload.getMaxWorkers());
            }
        }

        synchronized (workloadStatsMap) {
            WorkloadStats info = getOrCreateWorkloadStats(workload);
            info.setActiveWorkerLimit(activeWorkerLimit);
            while (info.getActiveWorkerCount() < minActiveWorkers) {
                addWorker(new SimulatedUser(this, workload), workload.calcNextThinkTimeMs());
            }
            return new WorkloadStats(info);
        }
    }

    public void removeAll() {
        synchronized (workloadStatsMap) {
            threadPool.shutdownNow();
            threadPool = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 10);

            for (WorkloadStats stats : workloadStatsMap.values()) {
                stats.setActiveWorkerLimit(0);
                stats.setActiveWorkerCount(0);
            }
        }
    }

    public Map<String, WorkloadStats> getWorkloadStats() {
        return workloadStatsMap;
    }

    public int getActiveWorkerLimit() {
        int limit = 0;
        synchronized (workloadStatsMap) {
            for (WorkloadStats workload : workloadStatsMap.values()) {
                if (workload.getActiveWorkerLimit() != null) {
                    limit += workload.getActiveWorkerLimit();
                }
            }
        }
        return limit;
    }

    public void stopAll() {
        synchronized (workloadStatsMap) {
            for (WorkloadStats workload : workloadStatsMap.values()) {
                workload.setActiveWorkerLimit(0);
            }
        }
    }

    public Map<WorkloadStep, WorkloadStepStats> getWorkloadStepStats() {
        Map<WorkloadStep, WorkloadStepStats> map = new TreeMap<WorkloadStep, WorkloadStepStats>(ToStringComparator.getComparator());
        for (Map.Entry<WorkloadStep, AtomicInteger> stepEntry : stepCompletionCounts.entrySet()) {
            WorkloadStepStats stepStats = new WorkloadStepStats();
            stepStats.setCompletionCount(stepEntry.getValue().get());
            map.put(stepEntry.getKey(), stepStats);
        }
        return map;
    }

    public boolean addWorker(final IWorker worker, long startDelayMs) {
        synchronized (workloadStatsMap) {
            WorkloadStats info = getOrCreateWorkloadStats(worker.getWorkload());
            if (!info.canAddWorker()) {
                info.setKilledWorkerCount(info.getKilledWorkerCount() + 1);
                return false;
            }
            info.setActiveWorkerCount(info.getActiveWorkerCount() + 1);
        }
        RunnableWorker runnableWorker = new RunnableWorker(worker);
        if (runnableWorker.expectedDequeueTimeMs == 0) {
        	runnableWorker.expectedDequeueTimeMs = System.currentTimeMillis() + startDelayMs;
        }
        threadPool.schedule(runnableWorker, startDelayMs, TimeUnit.MILLISECONDS);
        return true;
    }

    public IStorefrontService getService() {
        return svc;
    }

    public void incrementStepCompletionCount(WorkloadStep step) {
        stepCompletionCounts.get(step).incrementAndGet();
    }

    public StorefrontStatsReport getStorefrontStatsReport() {
        StorefrontStatsReport report = new StorefrontStatsReport();

        svc.getAppInstance().setCpuUtilization(PerformanceUtil.getAvgCpuUtilization());

        report.setTimestamp(Calendar.getInstance());
        report.setAppInstance(svc.getAppInstance());
        report.setTransactionStats(svc.getTransactionStats());
        report.setWorkloadStats(getWorkloadStats());
        report.setWorkloadStepStats(getWorkloadStepStats());
        
        return report;
    }

    /**
     * You must have a lock on workloadStatsMap to call this method.
     */
    protected WorkloadStats getOrCreateWorkloadStats(Workload workload) {
        synchronized (workloadStatsMap) {
            WorkloadStats stats = workloadStatsMap.get(workload.getName());
            if (stats == null) {
                stats = new WorkloadStats(workload);
                workloadStatsMap.put(workload.getName(), stats);
            }
            return stats;
        }
    }

    public Map<String, WorkloadStats> getAggregateWorkloadStats() {
		return aggregateWorkloadStats;
	}
    
    public void aggregateCompletedWorkerStats(Workload workload, WorkloadStats stats) {
    	if (!aggregateWorkloadStats.containsKey(workload.getName())) {
    		aggregateWorkloadStats.put(workload.getName(), new WorkloadStats(workload));
    	}
    	WorkloadStats workloadStats = aggregateWorkloadStats.get(workload.getName());
    	workloadStats.applyDeltas(stats);
    }

	protected class RunnableWorker implements Runnable {
        private final IWorker worker;
        private long completionWorkTimeMs;
        private final ScheduledThreadPoolExecutor originalThreadPool;
        private long expectedDequeueTimeMs;

        public RunnableWorker(IWorker worker) {
            this.worker = worker;
            this.originalThreadPool = threadPool;
        }

        public void run() {
            Workload workload = worker.getWorkload();

            // Verify this worker can still run
            synchronized (workloadStatsMap) {
            	WorkloadStats stats = getOrCreateWorkloadStats(workload);
                if (originalThreadPool != threadPool) {
                    stats.setActiveWorkerCount(stats.getActiveWorkerCount() - 1);
                    return;
                }
                if (stats.exceedsWorkerLimit()) {
                    // Don't run this worker. We're over the limit
                    stats.setActiveWorkerCount(stats.getActiveWorkerCount() - 1);
                    stats.setKilledWorkerCount(stats.getKilledWorkerCount() + 1);
                    return;
                }
            }

            BaseDao.setThreadTransactionStartTime(expectedDequeueTimeMs);

            // Run the worker
            long startTimeMs = (expectedDequeueTimeMs == 0) ? System.currentTimeMillis() : expectedDequeueTimeMs;
            long delay;
            boolean workerFailed = false;
            try {
                delay = worker.doWork();
                expectedDequeueTimeMs = 0;
            } catch (RetryWorkException e) {
                delay = e.getRetryDelayMs();
            } catch (Exception e) {
                delay = IWorker.COMPLETE_NO_REPEAT;
                workerFailed = true;
                logger.warn("Simulated worker failed", e);
            }
            long endTimeMs = System.currentTimeMillis();
            completionWorkTimeMs += (endTimeMs - startTimeMs);

            // Update stats
            synchronized (workloadStatsMap) {
            	WorkloadStats stats = getOrCreateWorkloadStats(workload);
                if (originalThreadPool != threadPool) {
                    stats.setActiveWorkerCount(stats.getActiveWorkerCount() - 1);
                    return;
                }
                stats.setWorkInvocationCount(stats.getWorkInvocationCount() + 1);
                stats.setTotalWorkTimeMs(stats.getTotalWorkTimeMs() + completionWorkTimeMs);
                if (delay < 0) {
                	if (!workerFailed) {
                        stats.setWorkCompletionCount(stats.getWorkCompletionCount() + 1);
                        stats.setTotalWorkCompletionTimeMs(stats.getTotalWorkCompletionTimeMs() + completionWorkTimeMs);
                        stats.setCompletedWorkerCount(stats.getCompletedWorkerCount() + 1);
                        completionWorkTimeMs = 0;
                	} else {                		
                		stats.setFailedWorkerCount(stats.getFailedWorkerCount() + 1);
                	}
                    // Determine whether this worker should run again
                    if (delay != IWorker.COMPLETE_NO_REPEAT && workload.isAutoRepeat()) {
                        delay = workload.calcNextThinkTimeMs();
                    }
                } else {
                	if (!workerFailed) {
                        stats.setWorkCompletionCount(stats.getWorkCompletionCount() + 1);
                        stats.setTotalWorkCompletionTimeMs(stats.getTotalWorkCompletionTimeMs() + completionWorkTimeMs);
                        stats.setCompletedWorkerCount(stats.getCompletedWorkerCount() + 1);
                        completionWorkTimeMs = 0;
                	} else {                		
                		stats.setFailedWorkerCount(stats.getFailedWorkerCount() + 1);
                	}
                }
                stats.setActiveWorkerCount(stats.getActiveWorkerCount() - 1);
                aggregateCompletedWorkerStats(workload, stats);
            }

            // Queue up next run
            if (delay >= 0) {
                addWorker(this.worker, delay);
            }
        }
    }
}
