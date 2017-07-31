/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.service;

import java.util.List;

import org.apache.log4j.Logger;

import com.nuodb.storefront.model.dto.StorefrontStats;
import com.nuodb.storefront.model.entity.AppInstance;

public interface IStorefrontService {
    public AppInstance getAppInstance();
    
    public Logger getLogger(Class<?> clazz);
    
    public StorefrontStats getStorefrontStats(final int maxCustomerIdleTimeSec, final Integer maxAgeSec);

    public List<AppInstance> getAppInstances(boolean activeOnly);
}
