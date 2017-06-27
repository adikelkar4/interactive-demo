/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.api;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.PutMetricDataResult;
import com.amazonaws.services.cloudwatch.model.StandardUnit;

import com.nuodb.storefront.StorefrontApp;
import com.nuodb.storefront.model.dto.*;

@Path("/stats")
public class StatsApi extends BaseApi {
    private static final String TRANSACTION_STATS_MAP_KEY = "transactionStats";
    private static final String WORKLOAD_STATS_MAP_KEY = "workloadStats";

    private static Map<String, Map<String, TransactionStats>> transactionStatHeap = new HashMap<>();

    private static final Integer statsPutMax = 500;
    private static Integer statsPutCount = 0;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public StorefrontStatsReport getAllStatsReport(@Context HttpServletRequest req) {
        StorefrontStatsReport rpt = getSimulator(req).getStorefrontStatsReport();
        DbFootprint footprint = getDbApi(req).getDbFootprint();

        rpt.setAppInstance(getTenant(req).getAppInstance());
        rpt.setDbStats(footprint);
        rpt.setWorkloadStats(workloadStatHeap.getOrDefault(NUODB_MAP_KEY, new HashMap<>()));
        rpt.setTransactionStats(transactionStatHeap.getOrDefault(NUODB_MAP_KEY, new HashMap<>()));
        clearWorkloadProperty(rpt.getWorkloadStats());

        return rpt;
    }

    @GET
    @Path("/storefront")
    @Produces(MediaType.APPLICATION_JSON)
    public StorefrontStats getStorefrontStats(@Context HttpServletRequest req, @QueryParam("sessionTimeoutSec") Integer sessionTimeoutSec, @QueryParam("maxAgeSec") Integer maxAgeSec) {
        int maxCustomerIdleTimeSec = (sessionTimeoutSec == null) ? StorefrontApp.DEFAULT_SESSION_TIMEOUT_SEC : sessionTimeoutSec;
        return getService(req).getStorefrontStats(maxCustomerIdleTimeSec, maxAgeSec);
    }

    @GET
    @Path("/transactions")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, TransactionStats> getTransactionStats(@Context HttpServletRequest req) {
        return transactionStatHeap.getOrDefault(NUODB_MAP_KEY, new HashMap<>());
    }

    @GET
    @Path("/workloads")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, WorkloadStats> getWorkloadStats(@Context HttpServletRequest req) {
        return workloadStatHeap.getOrDefault(NUODB_MAP_KEY, new HashMap<>());
    }

    @GET
    @Path("/workload-steps")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<WorkloadStep, WorkloadStepStats> getWorkloadStepStats(@Context HttpServletRequest req) {
        return getSimulator(req).getWorkloadStepStats();
    }

    @GET
    @Path("/db")
    @Produces(MediaType.APPLICATION_JSON)
    public DbFootprint getDbStats(@Context HttpServletRequest req) {
        return getDbApi(req).getDbFootprint();
    }

    @PUT
    @Path("/db")
    @Produces(MediaType.APPLICATION_JSON)
    public DbFootprint setDbStats(@Context HttpServletRequest req, @QueryParam("numRegions") Integer numRegions, @QueryParam("numHosts") Integer numHosts) {
        DbFootprint footprint = getDbApi(req).setDbFootprint(numRegions.intValue(), numHosts.intValue());

        return footprint;
    }

    @GET
    @Path("/regions")
    @Produces(MediaType.APPLICATION_JSON)
    public List<RegionStats> getRegionStats(@Context HttpServletRequest req) {
        return getDbApi(req).getRegionStats();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putContainerStats(@Context HttpServletRequest req, StatsPayload stats) {
        if (stats.getPayload().size() < 1) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Map<String, Map> payload = stats.getPayload();

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Integer>> tStats = (Map<String, Map<String, Integer>>)payload.getOrDefault(TRANSACTION_STATS_MAP_KEY, new HashMap<>());
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Integer>> wStats = (Map<String, Map<String, Integer>>)payload.getOrDefault(WORKLOAD_STATS_MAP_KEY, new HashMap<>());

        if (tStats.size() < 1 && wStats.size() < 1) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        // TODO - Break this off into its own threaded process, should only respond with acknowledged receipt of stats  - AndyM/KevinW
        synchronized (this.heapLock) { // Always synchronize on the heapLock so both maps are protected simultaneously
            if (!transactionStatHeap.containsKey(NUODB_MAP_KEY)) {
                transactionStatHeap.put(NUODB_MAP_KEY, new HashMap<>());
            }

            if(!workloadStatHeap.containsKey(NUODB_MAP_KEY)) {
                workloadStatHeap.put(NUODB_MAP_KEY, new HashMap<>());
            }

            Map<String, TransactionStats> tTmp = transactionStatHeap.get(NUODB_MAP_KEY);
            Map<String, WorkloadStats> wTmp = workloadStatHeap.get(NUODB_MAP_KEY);

            for (Map.Entry<String, Map<String, Integer>> entry : tStats.entrySet()) {
                if (tTmp.containsKey(entry.getKey())) {
                    tTmp.get(entry.getKey()).applyDeltas(entry.getValue());
                } else {
                	TransactionStats newStats = new TransactionStats();
                	newStats.applyDeltas(entry.getValue());
                    tTmp.put(entry.getKey(), newStats);
                }
            }

            for (Map.Entry<String, Map<String, Integer>> entry : wStats.entrySet()) {
                if (wTmp.containsKey(entry.getKey())) {
                    wTmp.get(entry.getKey()).applyDeltas(entry.getValue());
                } else {
                	WorkloadStats newStats = new WorkloadStats();
                	newStats.setActiveWorkerLimit(0);
                	newStats.applyDeltas(entry.getValue());
                    wTmp.put(entry.getKey(), newStats);
                }
            }

            if (this.statsPutCount >= this.statsPutMax) {
                this.statsPutCount = 0;
                int totalCount = 0;
                long totalDuration = 0;

                for (Map.Entry<String, TransactionStats> ts : tTmp.entrySet()) {
                    totalCount += ts.getValue().getTotalCount();
                    totalDuration += ts.getValue().getTotalDurationMs();
                }

                try {
                    AmazonCloudWatch cw = AmazonCloudWatchClientBuilder.defaultClient();
                    Dimension dimension = new Dimension()
                            .withName("AVG_LATENCY")
                            .withValue("MS");
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
                this.statsPutCount++;
            }
        }

        return Response.status(Response.Status.OK).build();
    }

    protected Map<String, WorkloadStats> clearWorkloadProperty(Map<String, WorkloadStats> statsMap)
    {
        // Clear unnecessary workload property to reduce payload size by ~25%
        for (WorkloadStats stats : statsMap.values()) {
            stats.setWorkload(null);
        }
        return statsMap;
    }
}
