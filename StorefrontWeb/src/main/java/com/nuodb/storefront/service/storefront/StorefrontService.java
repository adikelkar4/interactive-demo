/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.service.storefront;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.googlecode.genericdao.search.Filter;
import com.googlecode.genericdao.search.Search;
import com.nuodb.storefront.StorefrontTenantManager;
import com.nuodb.storefront.dal.IStorefrontDao;
import com.nuodb.storefront.dal.TransactionType;
import com.nuodb.storefront.model.dto.StorefrontStats;
import com.nuodb.storefront.model.entity.AppInstance;
import com.nuodb.storefront.service.IStorefrontService;
import com.nuodb.storefront.servlet.StorefrontWebApp;

/**
 * Basic implementation of the storefront service interface. Each service method invocation runs in its own transaction.
 */
public class StorefrontService implements IStorefrontService {
    private final AppInstance appInstance;
    private final IStorefrontDao dao;

    public StorefrontService(AppInstance appInstance, IStorefrontDao dao) {
        this.appInstance = appInstance;
        this.dao = dao;
    }

    @Override
    public AppInstance getAppInstance() {
        return appInstance;
    }

    @Override
    public Logger getLogger(Class<?> clazz) {
        return StorefrontTenantManager.getTenant(appInstance.getTenantName()).getLogger(clazz);
    }

    @Override
    public StorefrontStats getStorefrontStats(final int maxCustomerIdleTimeSec, final Integer maxAgeSec) {
        return dao.runTransaction(TransactionType.READ_ONLY, "getStorefrontStats", new Callable<StorefrontStats>() {
            @Override
            public StorefrontStats call() {
                return dao.getStorefrontStats(maxCustomerIdleTimeSec, maxAgeSec);
            }
        });
    }


    @Override
    public List<AppInstance> getAppInstances(final boolean activeOnly) {
        return dao.runTransaction(TransactionType.READ_ONLY, "getAppInstances", new Callable<List<AppInstance>>() {
            @SuppressWarnings("unchecked")
            @Override
            public List<AppInstance> call() {
                Search search = new Search(AppInstance.class);
                if (activeOnly) {
                    Calendar minLastHeartbeat = Calendar.getInstance();
                    minLastHeartbeat.add(Calendar.SECOND, -StorefrontWebApp.MAX_HEARTBEAT_AGE_SEC);
                    search.addFilter(Filter.greaterOrEqual("lastHeartbeat", minLastHeartbeat));
                }
                search.addSort("region", false);
                search.addSort("url", false);
                search.addSort("lastHeartbeat", true);
                List<AppInstance> instances = (List<AppInstance>) dao.search(search);

                // Perform instance list cleanup:
                // 1) For the local instance, use in-memory object (newer) rather than what's in DB (updated with every heartbeat)
                // 2) Remove extra instances with the same URL (instance with most recent heartbeat wins)
                String localUuid = appInstance.getUuid();
                boolean foundLocal = false;
                for (int i = 0; i < instances.size();) {
                    AppInstance instance = instances.get(i);
                    if (instance.getUuid().equals(localUuid)) {
                        instances.set(i, appInstance);
                        foundLocal = true;
                    } else if (activeOnly && i > 0 && instance.getUrl().equals(instances.get(i - 1).getUrl())) {
                        instances.remove(i);
                        continue;
                    }

                    i++;
                }

                if (!foundLocal) {
                    // Avoid race condition whereby the instance list is being requested before the first heartbeat
                    // by ensuring the local instance is always present in the list
                    instances.add(appInstance);
                }

                return instances;
            }
        });
    }
}
