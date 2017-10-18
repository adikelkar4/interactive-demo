/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.service.storefront;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.nuodb.storefront.StorefrontApp;
import com.nuodb.storefront.dal.IStorefrontDao;
import com.nuodb.storefront.dal.TransactionType;
import com.nuodb.storefront.exception.ApiException;
import com.nuodb.storefront.model.dto.ConnInfo;
import com.nuodb.storefront.model.dto.DbRegionInfo;
import com.nuodb.storefront.model.entity.AppInstance;
import com.nuodb.storefront.service.IStorefrontTenant;
import com.nuodb.storefront.util.PerformanceUtil;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.uri.UriComponent;
import com.sun.jersey.api.uri.UriComponent.Type;

public class HeartbeatService implements Runnable {
    private final Logger logger;
    private final IStorefrontTenant tenant;
    private int secondsUntilNextPurge = 0;
    private int consecutiveFailureCount = 0;
    private int successCount = 0;
    private Map<String, Set<URI>> wakeList = new HashMap<String, Set<URI>>();
    private Set<URI> warnList = new HashSet<URI>();
    private long cumGcTime = 0;
	public static final int GC_CUMULATIVE_TIME_LOG_MS = 500; // every 0.5 sec of cumulative GC time logged
	public static final int MIN_INSTANCE_PURGE_AGE_SEC = 60 * 60; // 1 hour
	public static final int STOP_USERS_AFTER_IDLE_UI_SEC = 60 * 10; // 10 min
	public static final int PURGE_FREQUENCY_SEC = 60 * 30; // 30 min

    public HeartbeatService(IStorefrontTenant tenant) {
        this.tenant = tenant;
        this.logger = tenant.getLogger(getClass());
    }

    public void run() {
        try {            
            final IStorefrontDao dao = tenant.createStorefrontDao();
            dao.runTransaction(TransactionType.READ_WRITE, "sendHeartbeat", new Runnable() {
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

                    // If enough time has elapsed, also delete rows of instances that are no longer sending heartbeats
                    if (secondsUntilNextPurge <= 0) {
                        Calendar maxLastHeartbeat = Calendar.getInstance();
                        maxLastHeartbeat.add(Calendar.SECOND, -HeartbeatService.MIN_INSTANCE_PURGE_AGE_SEC);
                        dao.deleteDeadAppInstances(maxLastHeartbeat);
                        secondsUntilNextPurge = HeartbeatService.PURGE_FREQUENCY_SEC;
                    }

                    // If interactive user has left the app, shut down any active workloads
                    Calendar idleThreshold = Calendar.getInstance();
                    idleThreshold.add(Calendar.SECOND, -HeartbeatService.STOP_USERS_AFTER_IDLE_UI_SEC);
                    if (appInstance.getStopUsersWhenIdle() && appInstance.getLastApiActivity().before(idleThreshold)) {
                        // Don't do any heavy lifting if there are no simulated workloads in progress
                        int activeWorkerCount = tenant.getSimulatorService().getActiveWorkerLimit();
                        if (activeWorkerCount > 0) {
                            // Check for idleness across *all* instances
                            if (dao.getActiveAppInstanceCount(idleThreshold) == 0) {
                                logger.info(appInstance.getTenantName() + ": Stopping all " + activeWorkerCount
                                        + " simulated users due to idle app instances.");
                                tenant.getSimulatorService().stopAll();
                            }
                        }
                    } else {
                        // We're still active, so if there are Storefronts to wake up, let's do it
                        wakeStorefronts();
                    }

                    consecutiveFailureCount = 0;
                    successCount++;
                }
            });
            
            long gcTime = PerformanceUtil.getGarbageCollectionTime();
            if (gcTime > cumGcTime + HeartbeatService.GC_CUMULATIVE_TIME_LOG_MS) {
                logger.info("Cumulative GC time of " + gcTime + " ms");
                cumGcTime = gcTime;
            }
        } catch (Exception e) {
            if (successCount > 0 && ++consecutiveFailureCount == 1) {
                logger.error(tenant.getAppInstance().getTenantName() + ": Unable to send heartbeat", e);
            }
        }
    }

    protected void wakeStorefronts() {
        // See if there's anything in the list to wake
        HashMap<String, Set<URI>> wakeListCopy;
        synchronized (wakeList) {
            if (wakeList.isEmpty()) {
                return;
            }
            wakeListCopy = new HashMap<String, Set<URI>>(wakeList);
        }

        // Get the best known scheme, port, and path for this Storefront instance.
        // We'll assume the others are running with the same settings.
        String sfScheme;
        int sfPort;
        String sfPath;
        try {
            URI homeUrl = new URI(tenant.getAppInstance().getUrl());
            sfScheme = homeUrl.getScheme();
            sfPort = homeUrl.getPort();
            sfPath = homeUrl.getPath();
            if (sfPath.endsWith("/")) {
                sfPath = sfPath.substring(0, sfPath.length() - 1);
            }
        } catch (URISyntaxException e1) {
            return;
        }

        String tenantName = tenant.getAppInstance().getTenantName();

        // Wake 1 Storefront in each region
        Client client = tenant.createApiClient();
        for (Map.Entry<String, Set<URI>> entry : wakeListCopy.entrySet()) {
            String region = entry.getKey();
            for (URI peerHostUrl : entry.getValue()) {
                URI peerStorefrontUrl;
                try {
                    peerStorefrontUrl = new URI(sfScheme, null, peerHostUrl.getHost(), sfPort, sfPath + "/api/app-instances/sync",
                            "tenant=" + UriComponent.encode(tenantName, Type.QUERY_PARAM), null);
                } catch (URISyntaxException e1) {
                    continue;
                }

                try {
                    client.resource(peerStorefrontUrl)
                            .type(MediaType.APPLICATION_JSON)
                            .put(ConnInfo.class, tenant.getDbConnInfo());
                    logger.info(tenantName + ": Successfully contacted peer Storefront at [" + peerStorefrontUrl + "] in the " + region + " region.");

                    // Success. We're done in this region.
                    break;
                } catch (Exception e) {
                    boolean warn = false;
                    if (!warnList.contains(peerStorefrontUrl)) {
                        warnList.add(peerStorefrontUrl);
                        warn = true;
                    }
                    if (warn) {
                        ApiException ae = ApiException.toApiException(e);
                        logger.warn(tenantName + ": Unable to contact peer Storefront [" + peerStorefrontUrl + "] in the " + region + " region: "
                                + ae.getMessage());
                    }
                }

                synchronized (wakeList) {
                    wakeList.remove(peerHostUrl);
                }
            }
        }
    }
}
