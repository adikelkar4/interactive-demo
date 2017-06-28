/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront;

import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.nuodb.storefront.model.dto.WorkloadStats;
import com.nuodb.storefront.model.dto.WorkloadStep;
import com.nuodb.storefront.model.dto.WorkloadStepStats;
import com.nuodb.storefront.service.ISimulatorService;
import com.nuodb.storefront.service.IStorefrontTenant;

public class StorefrontApp {
	static {
		// Try applying properties from specified file
		try {
			String propertyFile = System.getProperty("properties", null);
			if (propertyFile != null) {
				Properties overrides = new Properties();
				overrides.load(new FileInputStream(propertyFile));
				System.getProperties().putAll(overrides);
			}
		} catch (Exception e) {
			Logger.getLogger(StorefrontApp.class).warn("Failed to read properties file", e);
		}

		// For JSP page compilation, use Jetty compiler when available to avoid
		// JDK dependency
		System.setProperty("org.apache.jasper.compiler.disablejsr199", "true");
	}

	public static final String LOGGER_NAME_TENANT_SEP = ":";

	// Storefront timings
	public static final int HEARTBEAT_INTERVAL_SEC = 10;
	public static final int MAX_HEARTBEAT_AGE_SEC = 20;
	
	public static void main(String[] args) throws Exception {
		List<String> argsList = Arrays.asList(args);
		List<String> dbArgs = argsList.stream().filter(string -> string.contains("db.")).collect(Collectors.toList());
		List<String> appArgs = argsList.stream().filter(string -> string.contains("app.")).collect(Collectors.toList());
		List<String> workloadArgs = argsList.stream().filter(string -> string.contains("workload."))
				.collect(Collectors.toList());
		Map<String, String> dbSettings = new HashMap<>();
		Map<String, String> workloadSettings = new HashMap<>();
		Map<String, String> appSettings = new HashMap<>();

		dbArgs.forEach(setting -> dbSettings.put(setting.split("=")[0], setting.split("=")[1]));
		workloadArgs.forEach(setting -> workloadSettings.put(setting.split("=")[0], setting.split("=")[1]));
		appArgs.forEach(setting -> appSettings.put(setting.split("=")[0], setting.split("=")[1]));
		IStorefrontTenant tenant = StorefrontTenantManager.createTenant(dbSettings.get("db.name").split("@")[0], dbSettings);
		tenant.setAppSettings(appSettings);
		ISimulatorService simulator = tenant.getSimulatorService();
		executeTasks(simulator, workloadSettings);
		while(true) {
			Thread.sleep(HEARTBEAT_INTERVAL_SEC * 1000);
			printSimulatorStats(simulator, System.out);
		}
	}

	public static void executeTasks(ISimulatorService simulator, Map<String, String> workloadSettings)
			throws InterruptedException {
		for (String setting : workloadSettings.keySet()) {
			Thread taskThread = new Thread(new Runnable() {
				
				@Override
				public void run() {
					WorkloadStep step = WorkloadStep.valueOf(setting.split("\\.")[1].toUpperCase());
					simulator.adjustWorkers(simulator.getWorkloadStats().get(step.name()).getWorkload(),
							Integer.parseInt(workloadSettings.get(setting)), Integer.parseInt(workloadSettings.get(setting)));
				}
			});
			taskThread.start();
		}
	}
	
    private static void printSimulatorStats(ISimulatorService simulator, PrintStream out) {
        out.println();
        out.println(String.format("%-30s %8s %8s %8s %8s | %7s %9s %7s %9s", "Workload", "Active", "Failed", "Killed", "Complete", "Steps",
                "Avg (s)", "Work", "Avg (s)"));
        for (Map.Entry<String, WorkloadStats> statsEntry : simulator.getAggregateWorkloadStats().entrySet()) {
            String workloadName = statsEntry.getKey();
            WorkloadStats stats = statsEntry.getValue();

            out.println(String.format("%-30s %8d %8d %8d %8d | %7d %9.3f %7d %9.3f",
                    workloadName,
                    stats.getActiveWorkerCount(),
                    stats.getFailedWorkerCount(),
                    stats.getKilledWorkerCount(),
                    stats.getCompletedWorkerCount(),
                    stats.getWorkInvocationCount(),
                    (stats.getAvgWorkTimeMs() != null) ? stats.getAvgWorkTimeMs() / 1000f : null,
                    stats.getWorkCompletionCount(),
                    (stats.getAvgWorkCompletionTimeMs() != null) ? stats.getAvgWorkCompletionTimeMs() / 1000f : null));
        }

        out.println();
        out.println(String.format("%-25s %20s", "Step:", "# Completions:"));
        for (Map.Entry<WorkloadStep, WorkloadStepStats> statsEntry : simulator.getWorkloadStepStats().entrySet()) {
            WorkloadStep step = statsEntry.getKey();
            WorkloadStepStats stats = statsEntry.getValue();
            out.println(String.format("%-25s %20d", step.name(), stats.getCompletionCount()));
        }
    }
}
