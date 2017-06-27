/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.service.storefront;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.nuodb.storefront.StorefrontApp;
import com.nuodb.storefront.StorefrontTenantManager;
import com.nuodb.storefront.dal.IStorefrontDao;
import com.nuodb.storefront.dal.TransactionType;
import com.nuodb.storefront.exception.ApiException;
import com.nuodb.storefront.model.dto.ConnInfo;
import com.nuodb.storefront.model.dto.DbRegionInfo;
import com.nuodb.storefront.model.dto.RegionStats;
import com.nuodb.storefront.model.entity.AppInstance;
import com.nuodb.storefront.service.IHeartbeatService;
import com.nuodb.storefront.service.IStorefrontPeerService;
import com.nuodb.storefront.service.IStorefrontTenant;
import com.nuodb.storefront.util.PerformanceUtil;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.uri.UriComponent;
import com.sun.jersey.api.uri.UriComponent.Type;

public class HeartbeatService implements IHeartbeatService {
    private final Logger logger;
    private final IStorefrontTenant tenant;
    private int secondsUntilNextPurge = 0;
    private int consecutiveFailureCount = 0;
    private int successCount = 0;
    private long cumGcTime = 0;

    public HeartbeatService(IStorefrontTenant tenant) {
        this.tenant = tenant;
        this.logger = tenant.getLogger(getClass());
    }

    @Override
    public void run() {
        try {            
            final IStorefrontDao dao = tenant.createStorefrontDao();
            dao.runTransaction(TransactionType.READ_WRITE, "sendHeartbeat", new Runnable() {
                @Override
                public void run() {
                    Calendar now = Calendar.getInstance();
                    AppInstance appInstance = tenant.getAppInstance();
                    secondsUntilNextPurge -= StorefrontApp.HEARTBEAT_INTERVAL_SEC;

                    if (appInstance.getFirstHeartbeat() == null) {
                        appInstance.setFirstHeartbeat(now);
                        appInstance.setLastApiActivity(now);
                    }

                    // Send the heartbeat with the latest "last heartbeat time"
                    DbRegionInfo region = dao.getCurrentDbNodeRegion();
                    appInstance.setCpuUtilization(PerformanceUtil.getAvgCpuUtilization());
                    appInstance.setLastHeartbeat(now);
                    appInstance.setRegion(region.regionName);
                    appInstance.setNodeId(region.nodeId);
                    dao.save(appInstance); // this will create or update as appropriate

                    // If interactive user has left the app, shut down any active workloads
                    Calendar idleThreshold = Calendar.getInstance();
                    idleThreshold.add(Calendar.SECOND, -StorefrontApp.STOP_USERS_AFTER_IDLE_UI_SEC);
                    if (appInstance.getStopUsersWhenIdle() && appInstance.getLastApiActivity().before(idleThreshold)) {
                        // Don't do any heavy lifting if there are no simulated workloads in progress
                        int activeWorkerCount = tenant.getSimulatorService().getActiveWorkerLimit();
                        if (activeWorkerCount > 0) {
                            // Check for idleness across *all* instances
                            if (dao.getActiveAppInstanceCount(idleThreshold) == 0) {
                                logger.info(appInstance.getTenantName() + ": Stopping all " + activeWorkerCount
                                        + " simulated users due to idle app instances.");
                                //tenant.getSimulatorService().stopAll();
                            }
                        }
                    }

                    consecutiveFailureCount = 0;
                    successCount++;
                }
            });
            
            long gcTime = PerformanceUtil.getGarbageCollectionTime();
            if (gcTime > cumGcTime + StorefrontApp.GC_CUMULATIVE_TIME_LOG_MS) {
                logger.info("Cumulative GC time of " + gcTime + " ms");
                cumGcTime = gcTime;
            }
        } catch (Exception e) {
            if (successCount > 0 && ++consecutiveFailureCount == 1) {
                logger.error(tenant.getAppInstance().getTenantName() + ": Unable to send heartbeat", e);
            }
        }
    }
}
