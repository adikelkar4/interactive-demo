/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.service.storefront;

import java.util.Calendar;
import java.util.Map;

import com.nuodb.storefront.api.StatsApi;
import org.apache.log4j.Logger;

import com.nuodb.storefront.StorefrontApp;
import com.nuodb.storefront.dal.IStorefrontDao;
import com.nuodb.storefront.dal.TransactionType;
import com.nuodb.storefront.model.dto.DbRegionInfo;
import com.nuodb.storefront.model.entity.AppInstance;
import com.nuodb.storefront.service.IHeartbeatService;
import com.nuodb.storefront.service.IStorefrontTenant;
import com.nuodb.storefront.util.PerformanceUtil;
import com.storefront.workload.launcher.LambdaLauncher;
import com.nuodb.storefront.model.dto.*;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;

public class HeartbeatService implements IHeartbeatService {
    private final Logger logger;
    private final IStorefrontTenant tenant;
    private final int statsIncMax = 500;
    private int secondsUntilNextPurge = 0;
    private int consecutiveFailureCount = 0;
    private int successCount = 0;
    private long cumGcTime = 0;
    private int statsInc = 0;

    public HeartbeatService(IStorefrontTenant tenant) {
        this.tenant = tenant;
        this.logger = tenant.getLogger(getClass());
    }

    @Override
    public void run() {
        try {            
            final IStorefrontDao dao = tenant.createStorefrontDao();
            dao.runTransaction(TransactionType.READ_WRITE, "sendHeartbeat", new Runnable() {
                @Override
                public void run() {
                    Calendar now = Calendar.getInstance();
                    AppInstance appInstance = tenant.getAppInstance();
                    secondsUntilNextPurge -= StorefrontApp.HEARTBEAT_INTERVAL_SEC;

                    if (appInstance.getFirstHeartbeat() == null) {
                        appInstance.setFirstHeartbeat(now);
                        appInstance.setLastApiActivity(now);
                    }

                    // Send the heartbeat with the latest "last heartbeat time"
                    DbRegionInfo region = dao.getCurrentDbNodeRegion();
                    appInstance.setCpuUtilization(PerformanceUtil.getAvgCpuUtilization());
                    appInstance.setLastHeartbeat(now);
                    appInstance.setRegion(region.regionName);
                    appInstance.setNodeId(region.nodeId);
                    dao.save(appInstance); // this will create or update as appropriate

                    // If interactive user has left the app, shut down any active workloads
                    Calendar idleThreshold = Calendar.getInstance();
                    idleThreshold.add(Calendar.SECOND, -StorefrontApp.STOP_USERS_AFTER_IDLE_UI_SEC);
                    if (appInstance.getStopUsersWhenIdle() && appInstance.getLastApiActivity().before(idleThreshold)) {
                        // Don't do any heavy lifting if there are no simulated workloads in progress
                        int activeWorkerCount = tenant.getSimulatorService().getActiveWorkerLimit();
                        if (activeWorkerCount > 0) {
                            // Check for idleness across *all* instances
                            if (dao.getActiveAppInstanceCount(idleThreshold) == 0) {
                                logger.info(appInstance.getTenantName() + ": Stopping all " + activeWorkerCount
                                        + " simulated users due to idle app instances.");
                                //tenant.getSimulatorService().stopAll();
                            }
                        }
                    }

                    consecutiveFailureCount = 0;
                    successCount++;
                }
            });
            
            long gcTime = PerformanceUtil.getGarbageCollectionTime();
            if (gcTime > cumGcTime + StorefrontApp.GC_CUMULATIVE_TIME_LOG_MS) {
                logger.info("Cumulative GC time of " + gcTime + " ms");
                cumGcTime = gcTime;
            }
        } catch (Exception e) {
            if (successCount > 0 && ++consecutiveFailureCount == 1) {
                logger.error(tenant.getAppInstance().getTenantName() + ": Unable to send heartbeat", e);
            }
        }

        if (this.statsInc >= this.statsIncMax) {
            this.statsInc = 0;
            int totalCount = 0;
            long totalDuration = 0;
            Map<String, Map<String, TransactionStats>> tStats = StatsApi.getTransactionStatHeap();

            if (tStats.containsKey("nuodb")) {
                for (Map.Entry<String, TransactionStats> ts : tStats.get("nuodb").entrySet()) {
                    totalCount += ts.getValue().getTotalCount();
                    totalDuration += ts.getValue().getTotalDurationMs();
                }
            }

            try {
                AmazonCloudWatch cw = AmazonCloudWatchClientBuilder.defaultClient();
                Dimension dimension = new Dimension()
                        .withName("ClusterName")
                        .withValue(LambdaLauncher.getEcsClusterName());
                MetricDatum datum = new MetricDatum()
                        .withMetricName("MS")
                        .withUnit(StandardUnit.Milliseconds)
                        .withValue((double) (totalDuration / totalCount))
                        .withDimensions(dimension);
                PutMetricDataRequest pmdr = new PutMetricDataRequest()
                        .withNamespace("INSTANCE/METRICS")
                        .withMetricData(datum);
                cw.putMetricData(pmdr);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            this.statsInc++;
        }

        return;
    }
}
