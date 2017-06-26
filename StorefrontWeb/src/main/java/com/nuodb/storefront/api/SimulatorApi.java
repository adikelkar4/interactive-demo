/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.nuodb.storefront.StorefrontApp;
import com.nuodb.storefront.model.dto.*;
import com.nuodb.storefront.model.type.MessageSeverity;
import com.nuodb.storefront.service.ISimulatorService;
import com.storefront.workload.launcher.LambdaLauncher;
import com.storefront.workload.launcher.LocalLauncher;
import com.storefront.workload.launcher.UserLauncher;

@Path("/simulator")
public class SimulatorApi extends BaseApi {
    public SimulatorApi() {
    }

    @GET
    @Path("/workloads")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<WorkloadStats> getWorkloads(@Context HttpServletRequest req) {
        return getSimulator(req).getWorkloadStats().values();
    }

    @DELETE
    @Path("/workloads")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeAll(@Context HttpServletRequest req) {
        getSimulator(req).removeAll();
        return Response.ok().build();
    }
    
    private UserLauncher buildUserLauncher(HttpServletRequest req) {
    	UserLauncher launcher = null;
    	String host = req.getHeader("HOST");

    	if (host.contains("localhost")) {
			Map<String, String> dbSettings = new HashMap<>();
			dbSettings.put("db.name", StorefrontApp.DB_NAME);
			dbSettings.put("db.user", StorefrontApp.DB_USER);
			dbSettings.put("db.password", StorefrontApp.DB_PASSWORD);
			LocalLauncher local = new LocalLauncher();
			local.setDbOptions(dbSettings);
			Map<String, String> appSettings = new HashMap<>();
			appSettings.put("app.host", host);
			local.setAppOptions(appSettings);
			launcher = local;
    	} else {
    		launcher = new LambdaLauncher();
    	}

    	return launcher;
    }

    @POST
    @Path("/workloads/{workload}/workers")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addWorkers(@Context HttpServletRequest req, @PathParam("workload") String workload, @FormParam("numWorkers") int numWorkers,
            @FormParam("entryDelayMs") int entryDelayMs) {
        getSimulator(req).addWorkers(lookupWorkloadByName(req, workload), numWorkers, entryDelayMs);
        return Response.ok().build();
    }

    @PUT
    @Path("/workloads/{workload}/workers")
    @Produces(MediaType.APPLICATION_JSON)
    public WorkloadStats adjustWorkers(@Context HttpServletRequest req, @PathParam("workload") String workload, @FormParam("minWorkers") int minWorkers,
            @FormParam("limit") Integer limit) {
        return getSimulator(req).adjustWorkers(lookupWorkloadByName(req, workload), minWorkers, limit);
    }

    @GET
    @Path("/steps")
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<WorkloadStep> getWorkloadSteps(@Context HttpServletRequest req) {
        return getSimulator(req).getWorkloadStepStats().keySet();
    }

    @POST
    @Path("/increaseUserCount")
    @Produces(MediaType.APPLICATION_JSON)
    public Response increaseUserCount(@Context HttpServletRequest req) {
        if (userContainerCount < 5) {
            moveUserCount(req, ++userContainerCount);

            return Response.ok().build();
        }

        return Response.status(Response.Status.FORBIDDEN).build();
    }

    @POST
    @Path("/decreaseUserCount")
    @Produces(MediaType.APPLICATION_JSON)
    public Response decreaseUserCount(@Context HttpServletRequest req) {
        if (userContainerCount > 0) {
            moveUserCount(req, --userContainerCount);
            decreaseWorkloadUserCounts(-1);

            return Response.ok().build();
        }

        return Response.status(Response.Status.FORBIDDEN).build();
    }

    @POST
    @Path("/zeroUserCount")
    @Produces(MediaType.APPLICATION_JSON)
    public Response zeroUserCount(@Context HttpServletRequest req) {
        userContainerCount = 0;
        moveUserCount(req, userContainerCount);
        clearWorkloadUserCounts();

        return Response.ok().build();
    }

    protected void moveUserCount(HttpServletRequest req, int count) {
        UserLauncher containerLauncher = this.buildUserLauncher(req);

        try {
            containerLauncher.launchUser(workloadDistribution, count);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return;
    }

    protected void decreaseWorkloadUserCounts(int containerChange) {
        synchronized (this.heapLock) { // Always synchronize on the heapLock so both maps are protected simultaneously
            if (!workloadStatHeap.containsKey(NUODB_MAP_KEY)) {
                workloadStatHeap.put(NUODB_MAP_KEY, new HashMap<>());
            }

            Map<String, WorkloadStats> wTmp = workloadStatHeap.get(NUODB_MAP_KEY);

            for (Map.Entry<String, WorkloadStats> stat : wTmp.entrySet()) {
                int distributionCount;

                try {
                    distributionCount = Integer.parseInt(workloadDistribution.get(stat.getKey()));
                } catch (NumberFormatException e) {
                    continue;
                }

                stat.getValue().setActiveWorkerLimit(stat.getValue().getActiveWorkerLimit() + (containerChange * distributionCount));
                stat.getValue().setActiveWorkerCount(stat.getValue().getActiveWorkerCount() + (containerChange * distributionCount));
            }
        }

        return;
    }

    protected void clearWorkloadUserCounts() {
        synchronized (this.heapLock) { // Always synchronize on the heapLock so both maps are protected simultaneously
            if (!workloadStatHeap.containsKey(NUODB_MAP_KEY)) {
                workloadStatHeap.put(NUODB_MAP_KEY, new HashMap<>());
            }

            Map<String, WorkloadStats> wTmp = workloadStatHeap.get(NUODB_MAP_KEY);

            for (Map.Entry<String, WorkloadStats> stat : wTmp.entrySet()) {
                stat.getValue().setActiveWorkerLimit(0);
                stat.getValue().setActiveWorkerCount(0);
            }
        }

        return;
    }

    protected Workload lookupWorkloadByName(@Context HttpServletRequest req, String name) {
        try {
            Workload workload = getSimulator(req).getWorkload(name);
            if (workload != null) {
                return workload;
            }
        } catch (Exception e) {
        }
        throw new IllegalArgumentException("Unknown workload '" + name + "'");
    }
}
