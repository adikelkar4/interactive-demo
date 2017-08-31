/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.service.storefront;

import java.util.Map;

import org.apache.log4j.Logger;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.nuodb.storefront.api.StatsApi;
import com.nuodb.storefront.model.dto.TransactionStats;
import com.nuodb.storefront.service.IStorefrontTenant;
import com.nuodb.storefront.servlet.StorefrontWebApp;
import com.nuodb.storefront.util.PerformanceUtil;
import com.storefront.workload.launcher.LambdaLauncher;

public class HeartbeatService implements Runnable {
    private final Logger logger;
    private final IStorefrontTenant tenant;
    private int consecutiveFailureCount = 0;
    private int successCount = 0;
    private long cumGcTime = 0;
    private long cumCwTime = 0;
    private int lastCount = 0;
    private long lastDuration = 0;

    public HeartbeatService(IStorefrontTenant tenant) {
        this.tenant = tenant;
        this.logger = tenant.getLogger(getClass());
    }

    @Override
    public void run() {
        try {
            long gcTime = PerformanceUtil.getGarbageCollectionTime();
            if (gcTime > cumGcTime + StorefrontWebApp.GC_CUMULATIVE_TIME_LOG_MS) {
                logger.info("Cumulative GC time of " + gcTime + " ms");
                cumGcTime = gcTime;
            }
        } catch (Exception e) {
            if (successCount > 0 && ++consecutiveFailureCount == 1) {
                logger.error(tenant.getAppInstance().getTenantName() + ": Unable to send heartbeat", e);
            }
        }

        long cwTime = System.currentTimeMillis();

        if (cwTime > (cumCwTime + StorefrontWebApp.CW_METRIC_LOG_TIME)) {
            cumCwTime = cwTime;
            int totalCount = 0;
            long totalDuration = 0;
            int tCount = 0;
            long tDur = 0;
            Map<String, Map<String, TransactionStats>> tStats = StatsApi.getTransactionStatHeap();

            if (tStats.containsKey("nuodb")) {
                synchronized (StatsApi.heapLock) {
                    for (Map.Entry<String, TransactionStats> ts : tStats.get("nuodb").entrySet()) {
                        tCount += ts.getValue().getTotalCount();
                        tDur += ts.getValue().getTotalDurationMs();
                    }
                }
            }

            if (tCount > 0) {
                totalCount = tCount - lastCount;
                totalDuration = tDur - lastDuration;
                lastCount = tCount;
                lastDuration = tDur;
            } else {
                totalCount = 0;
                totalDuration = 0;
                lastCount = 0;
                lastDuration = 0;
            }

            try {
                double avg = (totalCount < 1) ? 0 : (totalDuration / totalCount);
                if (!tenant.getDbConnInfo().getHost().contains("localhost")) {
                    AmazonCloudWatch cw = AmazonCloudWatchClientBuilder.defaultClient();
                    Dimension dimension = new Dimension()
                            .withName("ClusterName")
                            .withValue(LambdaLauncher.getEcsClusterName());
                    MetricDatum datum = new MetricDatum()
                            .withMetricName("AverageLatency")
                            .withUnit(StandardUnit.Milliseconds)
                            .withValue(avg)
                            .withDimensions(dimension);
                    PutMetricDataRequest pmdr = new PutMetricDataRequest()
                            .withNamespace("INSTANCE/METRICS")
                            .withMetricData(datum);
                    cw.putMetricData(pmdr);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return;
    }
}
