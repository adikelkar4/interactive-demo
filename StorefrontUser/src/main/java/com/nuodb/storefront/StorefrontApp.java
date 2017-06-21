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

import com.nuodb.storefront.model.dto.Workload;
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

	// Storefront properties
	public static final String IP_DETECT_URL = System.getProperty("storefront.ipDetectUrl",
			"http://checkip.amazonaws.com");
	public static final String DEFAULT_URL = System.getProperty("storefront.url",
			"{protocol}://{host}:{port}/{context}");
	public static final int DEFAULT_PORT = Integer.valueOf(System.getProperty("maven.tomcat.port", "9001"));
	public static final String APP_NAME = System.getProperty("storefront.name", "NuoDB Storefront Demo");
	public static final String TENANT_PARAM_NAME = "tenant";
	public static final String LOGGER_NAME_TENANT_SEP = ":";

	// Storefront timings
	public static final int HEARTBEAT_INTERVAL_SEC = 10;
	public static final int CPU_SAMPLING_INTERVAL_SEC = 1;
	public static final int MAX_HEARTBEAT_AGE_SEC = 20;
	public static final int PURGE_FREQUENCY_SEC = 60 * 30; // 30 min
	public static final int STOP_USERS_AFTER_IDLE_UI_SEC = 60 * 10; // 10 min
	public static final int MIN_INSTANCE_PURGE_AGE_SEC = 60 * 60; // 1 hour
	public static final int DEFAULT_SESSION_TIMEOUT_SEC = 60 * 20;// 20 min
	public static final int DEFAULT_ANALYTIC_MAX_AGE = 60 * 30;// 30 min
	public static final int BENCHMARK_DURATION_MS = 10000;
	public static final int SIMULATOR_STATS_DISPLAY_INTERVAL_MS = 5000;
	public static final int GC_CUMULATIVE_TIME_LOG_MS = 500; // every 0.5 sec of
																// cumulative GC
																// time logged

	// Database properties
	public static final String DB_NAME = System.getProperty("storefront.db.name");
	public static final String DB_USER = System.getProperty("storefront.db.user");
	public static final String DB_PASSWORD = System.getProperty("storefront.db.password");
	public static final String DB_OPTIONS = System.getProperty("storefront.db.options");
	public static final int DB_PING_TIMEOUT_SEC = Integer
			.valueOf(System.getProperty("storefront.db.pingTimeoutSec", "0")); // 0=disabled
	public static final int DB_MAX_INIT_WAIT_TIME_SEC = Integer
			.valueOf(System.getProperty("storefront.db.initWaitTimeSec", "5"));
	public static final String DB_DOMAIN_BROKER = System.getProperty("domain.broker", "localhost");
	public static final String DB_PROCESS_TAG = System.getProperty("storefront.db.processTag", "demo_${db.name}");

	// Database API properties
	public static final int DBAPI_READ_TIMEOUT_SEC = Integer
			.valueOf(System.getProperty("storefront.dbapi.readTimeoutSec", "10"));
	public static final int DBAPI_CONNECT_TIMEOUT_SEC = Integer
			.valueOf(System.getProperty("storefront.dbapi.connectTimeoutSec", "10"));
	public static final int DBAPI_MAX_UNAVAILABLE_RETRY_TIME_SEC = Integer
			.valueOf(System.getProperty("storefront.dbapi.unavailableRetryTimeSec", "3"));
	public static final String DBAPI_HOST = System.getProperty("storefront.dbapi.host");
	public static final int DBAPI_PORT = Integer.valueOf(System.getProperty("storefront.dbapi.port", "8888"));
	public static final String DBAPI_USERNAME = System.getProperty("storefront.dbapi.user", "domain");
	public static final String DBAPI_PASSWORD = System.getProperty("storefront.dbapi.password", "bird");

	// Other properties
	public static final int SQLEXPLORER_PORT = Integer
			.valueOf(System.getProperty("storefront.sqlexplorer.port", "9001"));

	// Default names
	public static final String DEFAULT_DB_NAME = "Storefront";
	public static final String DEFAULT_DB_HOST = "localhost";
	public static final String DEFAULT_REGION_NAME = "Unknown region";
	public static final String DEFAULT_TENANT_NAME = "Default";

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
			WorkloadStep step = WorkloadStep.valueOf(setting.split("\\.")[1].toUpperCase());
			simulator.adjustWorkers(simulator.getWorkloadStats().get(step.name()).getWorkload(),
					Integer.parseInt(workloadSettings.get(setting)), Integer.parseInt(workloadSettings.get(setting)));
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
