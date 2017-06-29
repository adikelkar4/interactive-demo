/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.dal;

import com.nuodb.storefront.model.dto.DbRegionInfo;
import com.nuodb.storefront.model.dto.StorefrontStats;
import com.nuodb.storefront.model.entity.IEntity;
import com.nuodb.storefront.model.type.Currency;
import com.nuodb.storefront.service.IStorefrontService;

/**
 * General-purpose DAO with a few specialized methods to interact with the Storefront database. This interface and associated implementation(s) should
 * be used by Storefront services only.
 */
public interface IStorefrontDao extends IBaseDao {
    public void initialize(IEntity entity);

    /**
     * Evicts the model from a DAO session so that subsequent changes are not committed to the database.
     */
    public void evict(IEntity model);

    /**
     * @see IStorefrontService#getStorefrontStats
     */
    public StorefrontStats getStorefrontStats(int maxCustomerIdleTimeSec, Integer maxAgeSec);

    /**
     * Gets the "georegion" tag of the NuoDB Transaction Engine of the current database connection. Since the Storefront uses a thread pool and may
     * communicate with multiple Transaction Engines, the return value may vary if called multiple times. An exception is thrown if the underlying
     * database does not support georegion metadata (i.e. NuoDB pre-2.0 etc.).
     */
    public DbRegionInfo getCurrentDbNodeRegion();

    /**
     * Gets the currency currently associated with a specified region. This is determined by looking at the most recent Storefront instance (by last
     * heartbeat time) associated with this region. If no such instance exists, <code>null</code> is returned instead.
     */
    public Currency getRegionCurrency(String region);
    
}
