/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.api;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.nuodb.storefront.StorefrontTenantManager;
import com.nuodb.storefront.model.dto.LogEntry;
import com.nuodb.storefront.model.entity.AppInstance;
import com.nuodb.storefront.model.type.Currency;

@Path("/app-instances")
public class AppInstanceApi extends BaseApi {
	public static List<LogEntry> activityLog = new ArrayList<>();

    public AppInstanceApi() {
    }

    @GET
    @Path("/init-params")
    public Map<String, String> getParams(@Context HttpServletRequest req) {
        Map<String, String> map = new HashMap<String, String>();

        ServletContext ctx = req.getServletContext();
        Enumeration<String> names = ctx.getInitParameterNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            map.put("init." + name, ctx.getInitParameter(name));
        }

        names = ctx.getAttributeNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            map.put("attr." + name, ctx.getAttribute(name) + "");
        }

        return map;
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public AppInstance updateAppInstance(@Context HttpServletRequest req, @FormParam("currency") Currency currency, @FormParam("stopUsersWhenIdle") Boolean stopUsersWhenIdle) {
        AppInstance instance = StorefrontTenantManager.getTenant(req).getAppInstance();
        if (currency != null) {
            instance.setCurrency(currency);
        }
        if (stopUsersWhenIdle != null) {
            instance.setStopUsersWhenIdle(stopUsersWhenIdle);
        }
        return instance;
    }

    @POST
    @Path("/log")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response putLog(Map<String, String> event) {
        synchronized (activityLog) {
            activityLog.add(new LogEntry(System.nanoTime(), event.get("Data")));
        }

        return Response.ok().build();
    }

    @GET
    @Path("/log")
    @Produces(MediaType.APPLICATION_JSON)
    public List<LogEntry> getLog(@Context HttpServletRequest req, @QueryParam("lastTime") Long lastTime) {
        long filterTime = lastTime == null ? 0 : lastTime;
        synchronized (activityLog) {
        	int subListStart = firstUnreportedEntry(activityLog, filterTime, 0, activityLog.size());
        	if (subListStart == -1) {
        		return new ArrayList<>();
        	}
        	return activityLog.subList(subListStart, activityLog.size());
        }
    }
    
    protected int firstUnreportedEntry(List<LogEntry> sortedList, long filterTime, int start, int end) {
    	if (sortedList.size() == 0) {
    		return -1;
    	}
    	int mid = start + (end-start)/2;
    	if (sortedList.get(mid).getTime() > filterTime) {
    		if (mid == 0 || mid == sortedList.size() - 1) {
    			return mid;
    		} else if (sortedList.get(mid-1).getTime() <= filterTime) {
    			return mid;
    		} else {
    			return firstUnreportedEntry(sortedList, filterTime, start, mid);
    		}
    	} else if (mid == sortedList.size() -1) {
    		return -1;
    	} else {
    		return firstUnreportedEntry(sortedList, filterTime, mid, end);
    	}
    }
}
