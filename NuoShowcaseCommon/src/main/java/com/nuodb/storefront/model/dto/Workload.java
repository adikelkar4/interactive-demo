/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.model.dto;

import java.util.ArrayList;
import java.util.List;

import com.nuodb.storefront.util.Randoms;

public class Workload {
    public static final int DEFAULT_MAX_WORKERS = 50000;

    public static final Workload BROWSER = new Workload(WorkloadStep.MULTI_BROWSE.name(), "Customer:  Browsing only", true, 750, 375, DEFAULT_MAX_WORKERS,
            WorkloadStep.MULTI_BROWSE);
    public static final Workload REVIEWER = new Workload(WorkloadStep.MULTI_BROWSE_AND_REVIEW.name(), "Customer:  Browsing & reviews", true, 1500, 750, DEFAULT_MAX_WORKERS,
            WorkloadStep.MULTI_BROWSE_AND_REVIEW);
    public static final Workload SHOPPER = new Workload(WorkloadStep.MULTI_SHOP.name(), "Customer:  Slow purchaser", true, 2000, 1000, DEFAULT_MAX_WORKERS, WorkloadStep.MULTI_SHOP);
    public static final Workload ANALYST = new Workload(WorkloadStep.ADMIN_RUN_REPORT.name(), "Back office analyst", true, 2000, 1000, DEFAULT_MAX_WORKERS, WorkloadStep.ADMIN_RUN_REPORT);

    private final String name;
    private final String description;
    private double avgThinkTimeMs;
    private double thinkTimeVariance;
    private boolean autoRepeat;
    private final WorkloadStep[] steps;
    private final Randoms rnd = new Randoms();
    private int maxWorkers;

    public Workload(String name) {
        this(name, name, false, 0, 0, DEFAULT_MAX_WORKERS);
    }

    public Workload(String name, String description, boolean autoRepeat, int avgThinkTimeMs, int thinkTimeStdDev, int maxWorkers, WorkloadStep... steps) {
        if (name == null) {
            throw new IllegalArgumentException("name");
        }

        this.name = name;
        this.description = description;
        this.autoRepeat = autoRepeat;
        this.avgThinkTimeMs = avgThinkTimeMs;
        this.thinkTimeVariance = Math.pow(thinkTimeStdDev, 2);
        this.maxWorkers = maxWorkers;

        try {
            List<WorkloadStep> expandedSteps = new ArrayList<WorkloadStep>();
            expandSteps(steps, expandedSteps);
            this.steps = expandedSteps.toArray(new WorkloadStep[expandedSteps.size()]);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public WorkloadStep[] getSteps() {
        return steps;
    }

    public long calcNextThinkTimeMs() {
        return Math.max(0L, Math.round(rnd.nextGaussian(avgThinkTimeMs, thinkTimeVariance)));
    }

    public String getName() {
        return name;
    }

    public double getAvgThinkTimeMs() {
        return avgThinkTimeMs;
    }

    public void setAvgThinkTimeMs(double avgThinkTimeMs) {
        this.avgThinkTimeMs = avgThinkTimeMs;
    }

    public double getThinkTimeVariance() {
        return thinkTimeVariance;
    }

    public void setThinkTimeVariance(double thinkTimeVariance) {
        this.thinkTimeVariance = thinkTimeVariance;
    }

    public boolean isAutoRepeat() {
        return autoRepeat;
    }

    public void setAutoRepeat(boolean autoRepeat) {
        this.autoRepeat = autoRepeat;
    }

    public int getMaxWorkers() {
        return maxWorkers;
    }

    public void setMaxWorkers(int maxWorkers) {
        this.maxWorkers = maxWorkers;
    }

    private static void expandSteps(WorkloadStep[] steps, List<WorkloadStep> accumulator) throws NoSuchFieldException {
        if (steps != null) {
            for (int i = 0; i < steps.length; i++) {
                WorkloadStep step = steps[i];
                WorkloadFlow subStepsAnnotation = step.getClass().getField(step.name()).getAnnotation(WorkloadFlow.class);
                if (subStepsAnnotation != null) {
                    expandSteps(subStepsAnnotation.steps(), accumulator);
                } else {
                    accumulator.add(step);
                }
            }
        }
    }
    
    @Override
    public String toString() {
        return name;
    }

	public String getDescription() {
		return description;
	}
}
