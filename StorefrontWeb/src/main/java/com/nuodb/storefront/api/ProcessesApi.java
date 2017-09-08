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

import com.nuodb.storefront.exception.ApiException;
import org.apache.log4j.Logger;

import com.nuodb.storefront.model.db.Process;
import com.nuodb.storefront.model.dto.ProcessDetail;
import com.storefront.workload.launcher.AwsHostLauncher;
import com.storefront.workload.launcher.HostLauncher;

@Path("/processes")
public class ProcessesApi extends BaseApi {
    public ProcessesApi() {
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

        try {
            getDbApi(req).increaseTeCount();
            log.warn("Host count increase requested");
        } catch (ApiException e) {
            return Response.serverError().build();
        }

        return Response.ok().build();
    }

    @POST
    @Path("/decreaseHostCount")
    @Produces(MediaType.APPLICATION_JSON)
    public Response decreaseHostCount(@Context HttpServletRequest req) {
        Logger log = getTenant(req).getLogger(this.getClass());

        try {
            getDbApi(req).decreaseTeCount();
            log.warn("Host count decrease requested");
        } catch (ApiException e) {
            return Response.serverError().build();
        }

        return Response.ok().build();
    }

    @POST
    @Path("/resetHostCount")
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetHostCount(@Context HttpServletRequest req) {
        Logger log = getTenant(req).getLogger(this.getClass());

        try {
            getDbApi(req).resetTeCount();
            log.info("Host count reset requested");
        } catch (ApiException e) {
            return Response.serverError().build();
        }

        return Response.ok().build();
    }

    @DELETE
    @Path("/{uid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Context HttpServletRequest req, @PathParam("uid") String uid) {
        getDbApi(req).shutdownProcess(uid);
        return Response.ok().build();
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
}