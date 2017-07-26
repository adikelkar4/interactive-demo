/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.api;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nuodb.storefront.model.dto.WorkloadStats;
import com.nuodb.storefront.model.dto.WorkloadStep;
import com.nuodb.storefront.model.entity.Customer;
import com.nuodb.storefront.service.IDbApi;
import com.nuodb.storefront.service.ISimulatorService;
import com.nuodb.storefront.service.IStorefrontService;
import com.nuodb.storefront.service.IStorefrontTenant;
import com.nuodb.storefront.servlet.BaseServlet;

public abstract class BaseApi {
    protected static final String NUODB_MAP_KEY = "nuodb";

    protected static Map<String, Map<String, WorkloadStats>> workloadStatHeap = new HashMap<>();
    public static final Object heapLock = new Object();

    protected static final Map<String, String> workloadDistribution;
    protected static int userContainerCount = 0;
    protected static int hostContainerCount = 1;

    static {
        workloadDistribution = new HashMap<>();
        workloadDistribution.put(WorkloadStep.MULTI_BROWSE.name(), "700");
        workloadDistribution.put(WorkloadStep.MULTI_BROWSE_AND_REVIEW.name(), "700");
        workloadDistribution.put(WorkloadStep.MULTI_SHOP.name(), "700");
        workloadDistribution.put(WorkloadStep.ADMIN_RUN_REPORT.name(), "0");
    }

    protected BaseApi() {
    }
    
    protected IStorefrontTenant getTenant(HttpServletRequest req) {
        return BaseServlet.getTenant(req);
    }

    protected IStorefrontService getService(HttpServletRequest req) {
        return BaseServlet.getStorefrontService(req);
    }

    protected IDbApi getDbApi(HttpServletRequest req) {
        return BaseServlet.getDbApi(req);
    }

    protected ISimulatorService getSimulator(HttpServletRequest req) {
        getTenant(req).getAppInstance().setLastApiActivity(Calendar.getInstance());
        return BaseServlet.getSimulator(req);
    }

    protected Customer getOrCreateCustomer(HttpServletRequest req, HttpServletResponse resp) {
        return BaseServlet.getOrCreateCustomer(req, resp);
    }
}
