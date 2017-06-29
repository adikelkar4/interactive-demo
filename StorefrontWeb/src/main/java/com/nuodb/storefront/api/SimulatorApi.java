/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.nuodb.storefront.model.dto.WorkloadStats;
import com.nuodb.storefront.servlet.StorefrontWebApp;
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
        return BaseApi.getWorkloadStatHeap().getOrDefault(NUODB_MAP_KEY, new HashMap<>()).values();
    }
    
    private UserLauncher buildUserLauncher(HttpServletRequest req) {
    	UserLauncher launcher = null;
    	String host = req.getHeader("HOST");

    	if (host.contains("localhost")) {
			Map<String, String> dbSettings = new HashMap<>();
			dbSettings.put("db.name", StorefrontWebApp.DB_NAME);
			dbSettings.put("db.user", StorefrontWebApp.DB_USER);
			dbSettings.put("db.password", StorefrontWebApp.DB_PASSWORD);
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
            if (!getWorkloadStatHeap().containsKey(NUODB_MAP_KEY)) {
                getWorkloadStatHeap().put(NUODB_MAP_KEY, new HashMap<>());
            }

            Map<String, WorkloadStats> wTmp = getWorkloadStatHeap().get(NUODB_MAP_KEY);

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
            if (!getWorkloadStatHeap().containsKey(NUODB_MAP_KEY)) {
                getWorkloadStatHeap().put(NUODB_MAP_KEY, new HashMap<>());
            }

            Map<String, WorkloadStats> wTmp = getWorkloadStatHeap().get(NUODB_MAP_KEY);

            for (Map.Entry<String, WorkloadStats> stat : wTmp.entrySet()) {
                stat.getValue().setActiveWorkerLimit(0);
                stat.getValue().setActiveWorkerCount(0);
            }
        }

        return;
    }
}
