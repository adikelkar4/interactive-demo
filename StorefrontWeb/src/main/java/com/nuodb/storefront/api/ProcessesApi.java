/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nuodb.storefront.launcher.AwsHostLauncher;
import com.nuodb.storefront.launcher.HostLauncher;
import com.nuodb.storefront.launcher.TourLauncher;
import com.nuodb.storefront.model.db.Process;
import com.nuodb.storefront.model.dto.ProcessDetail;

@Path("/processes")
public class ProcessesApi extends BaseApi {
	
	private static final Map<String, Map<String, Integer>> tourTopologies = new HashMap<>();
	private static Map<String, Integer> currentTopology = new HashMap<>();
	
	static {
		Map<String, Integer> comparisonTour = new HashMap<>();
		Map<String, Integer> scalingTour = new HashMap<>();
		scalingTour.put("SM", 1);
		scalingTour.put("TE", 1);
		scalingTour.put("MYSQL", 0);
		comparisonTour.put("SM", 1);
		comparisonTour.put("TE", 1);
		comparisonTour.put("MYSQL", 1);
		tourTopologies.put("tour-scale-out", scalingTour);
		tourTopologies.put("tour-database-comparison", comparisonTour);
		currentTopology.put("SM", 1);
		currentTopology.put("TE", 1);
		currentTopology.put("MYSQL", 0);
	}
	
	

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<ProcessDetail> getProcesses(@Context HttpServletRequest req) {
        int currentNodeId = getTenant(req).getAppInstance().getNodeId();

        // Fetch processes
        Map<Integer, ProcessDetail> processMap = new HashMap<Integer, ProcessDetail>();
        for (Process process : getDbApi(req).getDbProcesses()) {
            ProcessDetail detail;
            processMap.put(process.nodeId, detail = new ProcessDetail(process));
            
            if (process.nodeId == currentNodeId) {
                detail.setCurrentConnection(true);
            }
        }

        return processMap.values();
    }

    @POST
    @Path("/increaseHostCount")
    @Produces(MediaType.APPLICATION_JSON)
    public Response increaseHostCount(@Context HttpServletRequest req) {
        Logger log = getTenant(req).getLogger(this.getClass());

        if (hostContainerCount < 5) {
            moveHostCount(req, ++hostContainerCount);
            log.info("Host count increased by 1");
            currentTopology.put("TE", Math.max(currentTopology.get("TE") + 1, 5));

            return Response.ok().build();
        }

        log.warn("Host count increase requested when already maxed out");

        return Response.ok().build();
    }

    @POST
    @Path("/decreaseHostCount")
    @Produces(MediaType.APPLICATION_JSON)
    public Response decreaseHostCount(@Context HttpServletRequest req) {
        Logger log = getTenant(req).getLogger(this.getClass());

        if (hostContainerCount > 1) {
            moveHostCount(req, --hostContainerCount);
            log.info("Host count decreased by 1");
            currentTopology.put("TE", Math.max(currentTopology.get("TE") - 1, 1));
            return Response.ok().build();
        }

        log.warn("Host count decrease requested when already at minimum");

        return Response.ok().build();
    }

    @POST
    @Path("/resetHostCount")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetHostCount(@Context HttpServletRequest req) {
        Logger log = getTenant(req).getLogger(this.getClass());

        moveHostCount(req, 1);
        log.info("Host count reset to 1");
        currentTopology = new HashMap<>(tourTopologies.get("tour-scale-out"));

        return Response.ok().build();
    }

    @DELETE
    @Path("/{uid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Context HttpServletRequest req, @PathParam("uid") String uid) {
        getDbApi(req).shutdownProcess(uid);
        return Response.ok().build();
    }

    private void moveHostCount(HttpServletRequest req, int count) {
        // TODO - Replace this with up/down code

        return;
    }

    private HostLauncher buildHostLauncher(HttpServletRequest req) {
        HostLauncher launcher = null;
        String host = req.getHeader("HOST");

        if (host.contains("localhost")) {
            return launcher;
        }

        launcher = new AwsHostLauncher();

        return launcher;
    }
    
    public static boolean initializeTourInfrastructure(String tourName, HttpServletRequest req) {
    	Logger log = getTenant(req).getLogger(ProcessesApi.class);
    	if (tourTopologies.containsKey(tourName)) {
    		if (tourTopologies.get(tourName).equals(currentTopology)) {
    			return true;
    		} else {
    			log.info("Initializing tour: " + tourName);
    			try {
					return new TourLauncher().initializeTour(tourTopologies.get(tourName));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					return false;
				}
    		}
    	}
    	log.warn("Tour " + tourName + " has no associated topology.");
    	return false;
    }
}