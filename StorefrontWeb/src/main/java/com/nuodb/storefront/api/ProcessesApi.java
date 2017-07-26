/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import com.nuodb.storefront.model.db.Process;
import com.nuodb.storefront.model.dto.ProcessDetail;
import com.nuodb.storefront.model.entity.AppInstance;
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

        // Marry with AppInstances
        for (AppInstance appInstance : getService(req).getAppInstances(true)) {
            ProcessDetail detail = processMap.get(appInstance.getNodeId());
            if (detail != null) {
                detail.getAppInstances().add(appInstance.getUrl());
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
        HostLauncher launcher = this.buildHostLauncher(req);

        if (launcher == null) {
            return;
        }

        try {
            launcher.scaleHosts(count);
        } catch (Exception e) {
            e.printStackTrace();
        }

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
}