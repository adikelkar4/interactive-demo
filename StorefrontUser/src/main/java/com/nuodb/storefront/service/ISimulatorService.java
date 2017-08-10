/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.service;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.nuodb.storefront.model.dto.StorefrontStatsReport;
import com.nuodb.storefront.model.dto.Workload;
import com.nuodb.storefront.model.dto.WorkloadStats;
import com.nuodb.storefront.model.dto.WorkloadStep;
import com.nuodb.storefront.model.dto.WorkloadStepStats;

public interface ISimulatorService {
    /** Looks up a workload by name */
    public Workload getWorkload(String name);

    /**
     * Adjusts the workers associated with a workload.
     * 
     * @param workload
     *            The workload.
     * @param minActiveWorkers
     *            The minimum number of active workers that should exist before this method returns. Must be non-negative. If the current number of
     *            active workers is below this number, additional workers are added (with no entry delay) to bring the active worker count up to this
     *            number.
     * @param activeWorkerLimit
     *            The maximum number of active workers that can exist concurrently. If additional workers of this workload type are added via
     *            {@link #addWorkers(Workload, int, int)} or some other means , they are immediately killed.
     */
    public void adjustWorkers(Workload workload, int minActiveWorkers, Integer activeWorkerLimit);

    /**
     * Removes all workers across all workloads, including those currently running, and sets the active worker limit to 0 across all workloads.
     */
    public void removeAll();
    
    /**
     * Gets the sum of active worker limits across all workloads.  Workloads without limits do not contribute to this count.
     */
    public int getActiveWorkerLimit();
    
    /**
     * Sets the active worker limit of all workloads to 0.  Any workers in progress are drained asynchronously.
     */
    public void stopAll();

    public Map<String, WorkloadStats> getWorkloadStats();
    
    public Map<WorkloadStep, AtomicInteger> getStepCompletionCounts();

    public Map<WorkloadStep, WorkloadStepStats> getWorkloadStepStats();

    public StorefrontStatsReport getStorefrontStatsReport();    
}
