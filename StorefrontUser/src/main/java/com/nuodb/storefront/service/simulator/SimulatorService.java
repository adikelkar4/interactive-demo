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

import org.apache.log4j.Logger;

import com.nuodb.storefront.model.dto.StorefrontStatsReport;
import com.nuodb.storefront.model.dto.Workload;
import com.nuodb.storefront.model.dto.WorkloadStats;
import com.nuodb.storefront.model.dto.WorkloadStep;
import com.nuodb.storefront.model.dto.WorkloadStepStats;
import com.nuodb.storefront.service.ISimulatorService;
import com.nuodb.storefront.service.IStorefrontService;
import com.nuodb.storefront.util.PerformanceUtil;
import com.nuodb.storefront.util.ToStringComparator;

import javassist.Modifier;

public class SimulatorService implements ISimulator, ISimulatorService {
	private final Logger logger;
	private ScheduledThreadPoolExecutor threadPool;
	private final IStorefrontService svc;
	private final Map<String, WorkloadStats> workloadStatsMap = new HashMap<String, WorkloadStats>();
	private final Map<WorkloadStep, AtomicInteger> stepCompletionCounts = new HashMap<WorkloadStep, AtomicInteger>();

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
	}

	public Workload getWorkload(String name) {
		synchronized (workloadStatsMap) {
			WorkloadStats stats = workloadStatsMap.get(name);
			return (stats == null) ? null : stats.getWorkload();
		}
	}

	public void adjustWorkers(Workload workload, int minActiveWorkers, Integer activeWorkerLimit) {
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
				throw new IllegalArgumentException(
						"minActiveWorkers cannot exceed workload limit of " + workload.getMaxWorkers());
			}
			if (activeWorkerLimit != null && activeWorkerLimit.intValue() > workload.getMaxWorkers()) {
				throw new IllegalArgumentException(
						"activeWorkerLimit cannot exceed workload limit of " + workload.getMaxWorkers());
			}
		}

		WorkloadStats info = getOrCreateWorkloadStats(workload);
		info.setActiveWorkerLimit(activeWorkerLimit);
		while (info.getActiveWorkerCount() < minActiveWorkers) {
			SimulatedUser user = new SimulatedUser(this, workload);
			threadPool.scheduleAtFixedRate(user, workload.calcNextThinkTimeMs(), 5000, TimeUnit.MILLISECONDS);
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
					limit += workload.getActiveWorkerLimit();
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
		Map<WorkloadStep, WorkloadStepStats> map = new TreeMap<WorkloadStep, WorkloadStepStats>(
				ToStringComparator.getComparator());
		for (Map.Entry<WorkloadStep, AtomicInteger> stepEntry : stepCompletionCounts.entrySet()) {
			WorkloadStepStats stepStats = new WorkloadStepStats();
			stepStats.setCompletionCount(stepEntry.getValue().get());
			map.put(stepEntry.getKey(), stepStats);
		}
		return map;
	}

	public IStorefrontService getService() {
		return svc;
	}

	public void incrementStepCompletionCount(WorkloadStep step) {
		if (!getStepCompletionCounts().containsKey(step)) {
			getStepCompletionCounts().put(step, new AtomicInteger());
		}
		getStepCompletionCounts().get(step).incrementAndGet();
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

	public Map<WorkloadStep, AtomicInteger> getStepCompletionCounts() {
		return stepCompletionCounts;
	}

	public void updateWorkloadStats(Workload type, String state, long duration) {
		WorkloadStats stats = getOrCreateWorkloadStats(type);
		synchronized (workloadStatsMap) {
			switch (state.toUpperCase()) {
			case "COMPLETE":
				stats.setCompletedWorkerCount(stats.getCompletedWorkerCount() + 1);
				stats.setWorkCompletionCount(stats.getWorkCompletionCount() + 1);
				stats.setWorkInvocationCount(stats.getWorkInvocationCount() + 1);
				stats.setTotalWorkCompletionTimeMs(stats.getTotalWorkCompletionTimeMs() + duration);
				stats.setTotalWorkTimeMs(stats.getTotalWorkTimeMs() + duration);
				break;
			case "FAILED":
				stats.setFailedWorkerCount(stats.getFailedWorkerCount() + 1);
				stats.setWorkInvocationCount(stats.getWorkInvocationCount() + 1);
				stats.setTotalWorkTimeMs(stats.getTotalWorkTimeMs() + duration);
				break;
			}
		}
	}
}
