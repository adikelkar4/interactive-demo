/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.api;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.nuodb.storefront.model.dto.DbFootprint;
import com.nuodb.storefront.model.dto.StorefrontStatsReport;
import com.nuodb.storefront.model.dto.TransactionStats;
import com.nuodb.storefront.model.dto.WorkloadStats;
import com.nuodb.storefront.model.dto.WorkloadStep;
import com.nuodb.storefront.service.IDbApi;
import com.nuodb.storefront.service.IStorefrontService;
import com.nuodb.storefront.service.IStorefrontTenant;
import com.nuodb.storefront.servlet.BaseServlet;
import com.nuodb.storefront.util.PerformanceUtil;

public abstract class BaseApi {
    public static final String NUODB_MAP_KEY = "nuodb";

    private static Map<String, Map<String, WorkloadStats>> workloadStatHeap = new HashMap<>();
    protected final Object heapLock = new Object();

    protected static final Map<String, String> workloadDistribution;
    protected static int userContainerCount = 0;

	private static Map<String, Map<String, TransactionStats>> transactionStatHeap = new HashMap<>();

    static {
        workloadDistribution = new HashMap<>();
        workloadDistribution.put(WorkloadStep.MULTI_BROWSE.name(), "700");
        workloadDistribution.put(WorkloadStep.MULTI_BROWSE_AND_REVIEW.name(), "700");
        workloadDistribution.put(WorkloadStep.MULTI_SHOP.name(), "700");
        workloadDistribution.put(WorkloadStep.ADMIN_RUN_REPORT.name(), "0");
    }

    protected BaseApi() {
    }
    
    protected static IStorefrontTenant getTenant(HttpServletRequest req) {
        return BaseServlet.getTenant(req);
    }

    protected IStorefrontService getService(HttpServletRequest req) {
        return BaseServlet.getStorefrontService(req);
    }

    protected static IDbApi getDbApi(HttpServletRequest req) {
        return BaseServlet.getDbApi(req);
    }

	protected Map<String, WorkloadStats> clearWorkloadProperty(Map<String, WorkloadStats> statsMap) {
	    // Clear unnecessary workload property to reduce payload size by ~25%
	    for (WorkloadStats stats : statsMap.values()) {
	        stats.setWorkload(null);
	    }
	    return statsMap;
	}

	public static StorefrontStatsReport buildBaseStatsReport(HttpServletRequest req) {
		StorefrontStatsReport rpt = new StorefrontStatsReport();
	    DbFootprint footprint = getDbApi(req).getDbFootprint();
	    getTenant(req).getAppInstance().setCpuUtilization(PerformanceUtil.getAvgCpuUtilization());
	    rpt.setAppInstance(getTenant(req).getAppInstance());
	    rpt.setTimestamp(Calendar.getInstance());
	    rpt.setDbStats(footprint);
	    rpt.setWorkloadStats(getWorkloadStatHeap().getOrDefault(NUODB_MAP_KEY, new HashMap<>()));
	    rpt.setTransactionStats(getTransactionStatHeap().getOrDefault(NUODB_MAP_KEY, new HashMap<>()));
	    rpt.setWorkloadStepStats(new HashMap<>());
		return rpt;
	}

	public static Map<String, Map<String, WorkloadStats>> getWorkloadStatHeap() {
		return workloadStatHeap;
	}

	public static void setWorkloadStatHeap(Map<String, Map<String, WorkloadStats>> workloadStatHeap) {
		BaseApi.workloadStatHeap = workloadStatHeap;
	}

	public static Map<String, Map<String, TransactionStats>> getTransactionStatHeap() {
		return transactionStatHeap;
	}

	public static void setTransactionStatHeap(Map<String, Map<String, TransactionStats>> transactionStatHeap) {
		BaseApi.transactionStatHeap = transactionStatHeap;
	}
}
