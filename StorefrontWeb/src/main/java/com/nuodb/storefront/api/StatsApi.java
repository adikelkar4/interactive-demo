/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.api;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
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

    private Map<String, Map<String, TransactionStats>> transactionStatHeap = new HashMap<>();
    private Map<String, Map<String, WorkloadStats>> workloadStatHeap = new HashMap<>();
    private final Object heapLock = new Object();

    public StatsApi() {
        this.transactionStatHeap.put(NUODB_MAP_KEY, new HashMap<>());
        this.workloadStatHeap.put(NUODB_MAP_KEY, new HashMap<>());

        return;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public StorefrontStatsReport getAllStatsReport(@Context HttpServletRequest req) {
        StorefrontStatsReport rpt = getSimulator(req).getStorefrontStatsReport();
        DbFootprint footprint = getDbApi(req).getDbFootprint();

        rpt.setDbStats(footprint);
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
        return getService(req).getTransactionStats();
    }

    @GET
    @Path("/workloads")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, WorkloadStats> getWorkloadStats(@Context HttpServletRequest req) {
        return clearWorkloadProperty(getSimulator(req).getWorkloadStats());
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

    @PUT
    @Path("/put")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putContainerStats(@Context HttpServletRequest req, StatsPayload stats) {
        if (stats.getPayload().size() < 1) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Map<String, Map> payload = stats.getPayload();

        @SuppressWarnings("unchecked")
        Map<String, TransactionStats> tStats = (Map<String, TransactionStats>)payload.getOrDefault(TRANSACTION_STATS_MAP_KEY, new HashMap<>());
        @SuppressWarnings("unchecked")
        Map<String, WorkloadStats> wStats = (Map<String, WorkloadStats>)payload.getOrDefault(WORKLOAD_STATS_MAP_KEY, new HashMap<>());

        if (tStats.size() < 1 && wStats.size() < 1) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        synchronized (this.heapLock) { // Always synchronize on the heapLock so both maps are protected simultaneously
            if (!this.transactionStatHeap.containsKey(NUODB_MAP_KEY) || !this.workloadStatHeap.containsKey(NUODB_MAP_KEY)) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }

            Map<String, TransactionStats> tTmp = this.transactionStatHeap.get(NUODB_MAP_KEY);
            Map<String, WorkloadStats> wTmp = this.workloadStatHeap.get(NUODB_MAP_KEY);

            for (Map.Entry<String, TransactionStats> entry : tStats.entrySet()) {
                if (tTmp.containsKey(entry.getKey())) {
                    tTmp.get(entry.getKey()).applyDeltas(entry.getValue());
                } else {
                    tTmp.put(entry.getKey(), entry.getValue());
                }
            }

            for (Map.Entry<String, WorkloadStats> entry : wStats.entrySet()) {
                if (wTmp.containsKey(entry.getKey())) {
                    wTmp.get(entry.getKey()).applyDeltas(entry.getValue());
                } else {
                    wTmp.put(entry.getKey(), entry.getValue());
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
