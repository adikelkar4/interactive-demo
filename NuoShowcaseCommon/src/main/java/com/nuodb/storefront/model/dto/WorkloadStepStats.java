/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.model.dto;

import java.util.Map;

public class WorkloadStepStats {
    private int completionCount;

    public WorkloadStepStats() {
    }

    public int getCompletionCount() {
        return completionCount;
    }

    public void setCompletionCount(int completionCount) {
        this.completionCount = completionCount;
    }

    public void applyDeltas(Map<String, Integer> deltas) {
        if (deltas.get("completionCount") != null) {
            this.completionCount += deltas.get("completionCount");
        }

        return;
    }
}
