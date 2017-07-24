/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.api;

import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.nuodb.storefront.StorefrontApp;
import com.nuodb.storefront.model.dto.DbFootprint;
import com.nuodb.storefront.model.dto.RegionStats;
import com.nuodb.storefront.model.dto.StatsPayload;
import com.nuodb.storefront.model.dto.StorefrontStats;
import com.nuodb.storefront.model.dto.StorefrontStatsReport;
import com.nuodb.storefront.model.dto.TransactionStats;
import com.nuodb.storefront.model.dto.WorkloadStats;
import com.nuodb.storefront.model.dto.WorkloadStep;
import com.nuodb.storefront.model.dto.WorkloadStepStats;

@Path("/stats")
public class StatsApi extends BaseApi {
    private static final String TRANSACTION_STATS_MAP_KEY = "transactionStats";
    private static final String WORKLOAD_STATS_MAP_KEY = "workloadStats";

    private static Map<String, Map<String, TransactionStats>> transactionStatHeap = new HashMap<>();
    private static Map<String, Date> lastStatUpdate = new HashMap<>();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public StorefrontStatsReport getAllStatsReport(@Context HttpServletRequest req) {
        StorefrontStatsReport rpt = getSimulator(req).getStorefrontStatsReport();
        String dbType = req.getParameter("dbType") == null ? "nuodb" : req.getParameter("dbType");
        DbFootprint footprint = getDbApi(req).getDbFootprint();

        rpt.setAppInstance(getTenant(req).getAppInstance());
        rpt.setDbStats(footprint);
        rpt.setWorkloadStats(workloadStatHeap.getOrDefault(dbType, new HashMap<>()));
        rpt.setTransactionStats(transactionStatHeap.getOrDefault(dbType, new HashMap<>()));
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
        String dbType = req.getParameter("dbType") == null ? "nuodb" : req.getParameter("dbType");
        return transactionStatHeap.getOrDefault(dbType, new HashMap<>());
    }

    @GET
    @Path("/workloads")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, WorkloadStats> getWorkloadStats(@Context HttpServletRequest req) {
        String dbType = req.getParameter("dbType") == null ? "nuodb" : req.getParameter("dbType");
        return workloadStatHeap.getOrDefault(dbType, new HashMap<>());
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
    	Map<String, Map> payload = stats.getPayload();
        if (payload.size() < 1) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        } else if (lastStatUpdate.containsKey(stats.getUid())) {
        	if (!lastStatUpdate.get(stats.getUid()).before(stats.getTimestamp())) {
        		return Response.ok().build();
        	}
        }
        lastStatUpdate.put(stats.getUid(), stats.getTimestamp());

        Map<String, Map> payload = stats.getPayload();
        String databaseType = stats.getDatabaseType();

        @SuppressWarnings("unchecked")
        Map<String, Map<String, Integer>> tStats = (Map<String, Map<String, Integer>>)payload.getOrDefault(TRANSACTION_STATS_MAP_KEY, new HashMap<>());
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Integer>> wStats = (Map<String, Map<String, Integer>>)payload.getOrDefault(WORKLOAD_STATS_MAP_KEY, new HashMap<>());

        if (tStats.size() < 1 && wStats.size() < 1) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        // TODO - Break this off into its own threaded process, should only respond with acknowledged receipt of stats  - AndyM/KevinW
        synchronized (this.heapLock) { // Always synchronize on the heapLock so both maps are protected simultaneously
            if (!transactionStatHeap.containsKey(databaseType)) {
                transactionStatHeap.put(databaseType, new HashMap<>());
            }

            if(!workloadStatHeap.containsKey(databaseType)) {
                workloadStatHeap.put(databaseType, new HashMap<>());
            }

            Map<String, TransactionStats> tTmp = transactionStatHeap.get(databaseType);
            Map<String, WorkloadStats> wTmp = workloadStatHeap.get(databaseType);

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
        }

        return Response.status(Response.Status.OK).build();
    }

    public static Map<String, Map<String, TransactionStats>> getTransactionStatHeap() {
        return transactionStatHeap;
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
