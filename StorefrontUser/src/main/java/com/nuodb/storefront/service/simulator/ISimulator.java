/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.service.simulator;

import com.nuodb.storefront.model.dto.Workload;
import com.nuodb.storefront.model.dto.WorkloadStep;
import com.nuodb.storefront.service.IStorefrontService;

/**
 * Provides basic access to the simulator, which schedules workers to do bursts of work.
 */
public interface ISimulator {

    /**
     * Gets the Storefront service associated with this simulator. This is for convenience so workers need not manage their own service instances.
     */
    public IStorefrontService getService();

    public void incrementStepCompletionCount(WorkloadStep step);
    
    public void updateWorkloadStats(Workload type, String result, long duration);
}
