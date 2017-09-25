/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.service;

import java.util.List;

import com.nuodb.storefront.exception.ApiException;
import com.nuodb.storefront.model.db.Database;
import com.nuodb.storefront.model.db.Process;
import com.nuodb.storefront.model.dto.ConnInfo;
import com.nuodb.storefront.model.dto.DbFootprint;
import com.nuodb.storefront.model.dto.RegionStats;

/**
 * Interface to interact with NuoDB's RESTful API. For Storefront database-specific API calls, uses the database name associated with the connection
 * string on the Storefront database.
 */
public interface IDbApi {
    /**
     * Gets the username and URL used by this implementation when connecting to the NuoDB RESTful API.
     * For security, the password is not reported (but rather set to null).
     */
    ConnInfo getApiConnInfo();
    
    /**
     * Makes a read-only API request to test the connection.  Throws an exception if a valid response was not received. 
     */
    void testConnection();

    /**
     * Fetches information about the Storefront DB.
     */
    Database getDb() throws ApiException;

    /**
     * Fetches information about all the database processes running in support of the Storefront DB. This method returns an empty list if the
     * Storefront DB not running.
     */
    List<Process> getDbProcesses() throws ApiException;

    /**
     * Shuts down a NuoDB process (TE or SM) with the given UID (which is a globally unique identifier, not PID). You can use this to shut down any
     * NuoDB process, not just ones running the Storefront DB.
     */
    void shutdownProcess(String uid) throws ApiException;

    /**
     * Fetches some basic stats on the Storefront DB's footprint and the NuoDB domain overall.
     */
    DbFootprint getDbFootprint() throws ApiException;

    /**
     * Gets stats for all regions, including ones where the Storefront DB is not currently running.
     */
    List<RegionStats> getRegionStats() throws ApiException;

    /**
     * Increases the TE count by one for the currently connected domain.
     */
    void increaseTeCount() throws ApiException;

    /**
     * Decreases the TE count by one for the currently connected domain, never going beneath one TE.
     */
    void decreaseTeCount() throws ApiException;

    /**
     * Resets the TE count to 1 for the currently connected domain.
     */
    void resetTeCount() throws ApiException;
}
