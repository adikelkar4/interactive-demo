/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.api;

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

import com.nuodb.storefront.StorefrontApp;
import com.nuodb.storefront.model.dto.*;

@Path("/stats")
public class StatsApi extends BaseApi {
    private static final String NUODB_MAP_KEY = "nuodb";
    private static final String TRANSACTION_STATS_MAP_KEY = "transactionStats";
    private static final String WORKLOAD_STATS_MAP_KEY = "workloadStats";

    private static Map<String, Map<String, TransactionStats>> transactionStatHeap = new HashMap<>();
    private static Map<String, Map<String, WorkloadStats>> workloadStatHeap = new HashMap<>();
    private final Object heapLock = new Object();

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

        if (footprint.usedRegionCount > 1) {
            getTenant(req).getStorefrontPeerService().asyncWakeStorefrontsInOtherRegions();
        }

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

        if (footprint.usedRegionCount > 1) {
            getTenant(req).getStorefrontPeerService().asyncWakeStorefrontsInOtherRegions();
        }

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
