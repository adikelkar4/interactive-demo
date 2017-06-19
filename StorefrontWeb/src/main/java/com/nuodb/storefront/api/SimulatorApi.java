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
import com.nuodb.storefront.model.dto.Message;
import com.nuodb.storefront.model.dto.Workload;
import com.nuodb.storefront.model.dto.WorkloadStats;
import com.nuodb.storefront.model.dto.WorkloadStep;
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

    @PUT
    @Path("/workloads")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setWorkloads(@Context HttpServletRequest req, Map<String, String> formParams) {
        Map<String, String> workloadSettings = new HashMap<>();

        for (Map.Entry<String, String> param : formParams.entrySet()) {
            if (param.getKey().startsWith("workload-")) {
                String workloadName = param.getKey().substring(9);
                int quantity = Integer.parseInt(param.getValue());

                if (workloadStatHeap.containsKey(NUODB_MAP_KEY) && workloadStatHeap.get(NUODB_MAP_KEY).containsKey(workloadName)) {
                    workloadStatHeap.get(NUODB_MAP_KEY).get(workloadName).setActiveWorkerCount(quantity);
                    workloadStatHeap.get(NUODB_MAP_KEY).get(workloadName).setActiveWorkerLimit(quantity);
                }

                workloadSettings.put(workloadName, param.getValue());
            }
        }

        UserLauncher containerLauncher = this.buildUserLauncher(req);

        try {
			containerLauncher.launchUser(workloadSettings, 1);
		} catch (Exception e) {
			e.printStackTrace();
		}

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
        userContainerCount++;
        UserLauncher containerLauncher = this.buildUserLauncher(req);

        try {
            containerLauncher.launchUser(workloadDistribution, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Response.ok().build();
    }

    @POST
    @Path("/decreaseUserCount")
    @Produces(MediaType.APPLICATION_JSON)
    public Response decreaseUserCount(@Context HttpServletRequest req) {
        userContainerCount--;
        UserLauncher containerLauncher = this.buildUserLauncher(req);

        try {
            containerLauncher.launchUser(workloadDistribution, -1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Response.ok().build();
    }

    @POST
    @Path("/zeroUserCount")
    @Produces(MediaType.APPLICATION_JSON)
    public Response zeroUserCount(@Context HttpServletRequest req) {
        userContainerCount = 0;
        UserLauncher containerLauncher = this.buildUserLauncher(req);

        try {
            containerLauncher.launchUser(workloadDistribution, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Response.ok().build();
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
