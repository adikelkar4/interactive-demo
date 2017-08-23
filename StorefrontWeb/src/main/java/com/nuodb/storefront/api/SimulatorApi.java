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
			dbSettings.put("db.url", StorefrontWebApp.DB_NAME);
			dbSettings.put("db.user", StorefrontWebApp.DB_USER);
			dbSettings.put("db.password", StorefrontWebApp.DB_PASSWORD);
			LocalLauncher local = new LocalLauncher();
			local.setDbOptions(dbSettings);
			Map<String, String> appSettings = new HashMap<>();
			appSettings.put("app.host", "http://" + host + "/StorefrontWeb");
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
        	changeUserCounts(1);
            moveUserCount(req, ++userContainerCount);
            return Response.ok().build();
        }

        //AppInstanceApi apiapi = new AppInstanceApi();
        //apiapi.putLog(req, "More user workloads have been requested, should begin within 3 minutes");
        getTenant(req).getLogger(this.getClass()).info("User workload was increased by 1");

        return Response.status(Response.Status.FORBIDDEN).build();
    }

    @POST
    @Path("/decreaseUserCount")
    @Produces(MediaType.APPLICATION_JSON)
    public Response decreaseUserCount(@Context HttpServletRequest req) {
        if (userContainerCount > 0) {
        	changeUserCounts(-1);
            moveUserCount(req, --userContainerCount);

            return Response.ok().build();
        }

        //AppInstanceApi apiapi = new AppInstanceApi();
        //apiapi.putLog(req, "A decrease in the user workloads has been requested, should show shortly");
        getTenant(req).getLogger(this.getClass()).info("User workload was decreased by 1");

        return Response.status(Response.Status.FORBIDDEN).build();
    }

    @POST
    @Path("/zeroUserCount")
    @Produces(MediaType.APPLICATION_JSON)
    public Response zeroUserCount(@Context HttpServletRequest req) {
        userContainerCount = 0;
        changeUserCounts(0);
        moveUserCount(req, userContainerCount);

        //AppInstanceApi apiapi = new AppInstanceApi();
        //apiapi.putLog(req, "All user workloads have been cleared out, should be at 0 workload shortly");
        getTenant(req).getLogger(this.getClass()).info("User workload was set to 0");

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

    protected void changeUserCounts(int containerChange) {
        synchronized (this.heapLock) { // Always synchronize on the heapLock so both maps are protected simultaneously
            if (!getWorkloadStatHeap().containsKey(NUODB_MAP_KEY)) {
                HashMap<String, WorkloadStats> nuodbWorkloadStats = new HashMap<>();
                for (String key: workloadDistribution.keySet()) {
					nuodbWorkloadStats.put(key, new WorkloadStats());
                }
                getWorkloadStatHeap().put(NUODB_MAP_KEY, nuodbWorkloadStats);
            }

            Map<String, WorkloadStats> wTmp = getWorkloadStatHeap().get(NUODB_MAP_KEY);

            for (Map.Entry<String, WorkloadStats> stat : wTmp.entrySet()) {
                int distributionCount;

                try {
                    distributionCount = Integer.parseInt(workloadDistribution.get(stat.getKey()));
                } catch (NumberFormatException e) {
                    continue;
                }
                
                if (containerChange == 0) {
                	stat.getValue().setActiveWorkerLimit(0);
                	stat.getValue().setActiveWorkerCount(0);
                } else {                	
                	stat.getValue().setActiveWorkerLimit(stat.getValue().getActiveWorkerLimit() + (containerChange * distributionCount));
                	stat.getValue().setActiveWorkerCount(stat.getValue().getActiveWorkerCount() + (containerChange * distributionCount));
                }
            }
        }

        return;
    }
}
