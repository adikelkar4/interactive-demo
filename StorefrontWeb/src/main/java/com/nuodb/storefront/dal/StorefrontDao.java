/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.dal;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;

import org.hibernate.Hibernate;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.type.BigDecimalType;
import org.hibernate.type.StringType;

import com.googlecode.genericdao.search.SearchResult;
import com.nuodb.storefront.model.dto.Category;
import com.nuodb.storefront.model.dto.DbRegionInfo;
import com.nuodb.storefront.model.dto.StorefrontStats;
import com.nuodb.storefront.model.dto.TransactionStats;
import com.nuodb.storefront.model.entity.IEntity;
import com.nuodb.storefront.model.entity.Product;
import com.nuodb.storefront.model.entity.PurchaseSelection;
import com.nuodb.storefront.model.type.Currency;
import com.nuodb.storefront.model.type.ProductSort;
import com.nuodb.storefront.servlet.StorefrontWebApp;

/**
 * Data access object designed for storefront operations, built on top of a general-purpose DAO. The caller is responsible for wrapping DAO calls in
 * transactions, typically by using the {@link #runTransaction(Callable)} or {@link #runTransaction(Runnable)} method.
 */
public class StorefrontDao extends BaseDao implements IStorefrontDao {
    private final Map<String, TransactionStats> transactionStatsMap;

    public StorefrontDao(Map<String, TransactionStats> transactionStatsMap) {
        this.transactionStatsMap = transactionStatsMap;
    }

    @Override
    public void initialize(IEntity entity) {
        Hibernate.initialize(entity);
    }

    @Override
    public void evict(IEntity entity) {
        getSession().evict(entity);
    }

    public StorefrontStats getStorefrontStats(int maxCustomerIdleTimeSec, Integer maxAgeSec) {
        final String ALL_STATS_QUERY = "SELECT"
                + " (SELECT COUNT(*) FROM PRODUCT) AS PRODUCT_COUNT,"
                + " (SELECT COUNT(*) FROM (SELECT DISTINCT CATEGORY FROM PRODUCT_CATEGORY) AS A) AS CATEGORY_COUNT,"
                + " (SELECT COUNT(*) FROM PRODUCT_REVIEW) AS PRODUCT_REVIEW_COUNT,"
                + " (SELECT COUNT(*) FROM CUSTOMER) AS CUSTOMER_COUNT,"
                + " (SELECT COUNT(*) FROM CUSTOMER WHERE DATE_LAST_ACTIVE >= :MIN_ACTIVE_TIME) AS ACTIVE_CUSTOMER_COUNT,"
                + " (SELECT COUNT(*) FROM CUSTOMER WHERE WORKLOAD IS NULL AND DATE_LAST_ACTIVE >= :MIN_ACTIVE_TIME) AS ACTIVE_WEB_CUSTOMER_COUNT"
                + " FROM DUAL;";

        final String RECENT_STATS_QUERY = "SELECT"
                + " (SELECT COUNT(*) FROM PRODUCT) AS PRODUCT_COUNT,"
                + " (SELECT COUNT(*) FROM (SELECT DISTINCT CATEGORY FROM PRODUCT_CATEGORY) AS A) AS CATEGORY_COUNT,"
                + " (SELECT COUNT(*) FROM PRODUCT_REVIEW WHERE DATE_ADDED >= :MIN_MODIFIED_TIME) AS PRODUCT_REVIEW_COUNT,"
                + " (SELECT NULL AS CUSTOMER_COUNT FROM DUAL),"
                + " (SELECT COUNT(*) FROM CUSTOMER WHERE DATE_LAST_ACTIVE >= :MIN_ACTIVE_TIME) AS ACTIVE_CUSTOMER_COUNT,"
                + " (SELECT COUNT(*) FROM CUSTOMER WHERE DATE_LAST_ACTIVE >= :MIN_ACTIVE_TIME AND WORKLOAD IS NULL) AS ACTIVE_WEB_CUSTOMER_COUNT"
                + " FROM DUAL;";

        // Run query
        SQLQuery query = getSession().createSQLQuery((maxAgeSec == null) ? ALL_STATS_QUERY : RECENT_STATS_QUERY);
        setStorefrontStatsParameters(query, maxCustomerIdleTimeSec, maxAgeSec);
        Object[] result = (Object[]) query.uniqueResult();

        // Fill stats
        StorefrontStats stats = new StorefrontStats();
        stats.setProductCount(getIntValue(result[0]));
        stats.setCategoryCount(getIntValue(result[1]));
        stats.setProductReviewCount(getIntValue(result[2]));
        stats.setCustomerCount(getIntValue(result[3]));
        stats.setActiveCustomerCount(getIntValue(result[4]));
        stats.setActiveWebCustomerCount(getIntValue(result[5]));
//        stats.setCartItemCount(getIntValue(result[6]));
//        stats.setCartValue(getBigDecimalValue(result[7]));
//        stats.setPurchaseCount(getIntValue(result[8]));
//        stats.setPurchaseItemCount(getIntValue(result[9]));
//        stats.setPurchaseValue(getBigDecimalValue(result[10]));

//        Calendar cal = Calendar.getInstance();
//        cal.setTimeInMillis(getLongValue(result[11]));
//        stats.setDateStarted(cal);

        return stats;
    }

    protected static int getIntValue(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(value.toString());
    }

    protected static BigDecimal getBigDecimalValue(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof Number) {
            return new BigDecimal(((Number) value).doubleValue());
        }
        String str = value.toString();
        if (str.equalsIgnoreCase("NaN")) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.toString());
    }

    protected static long getLongValue(Object value) {
        return getBigDecimalValue(value).longValue();
    }

    @Override
    public DbRegionInfo getCurrentDbNodeRegion() {
        DbRegionInfo info = new DbRegionInfo();
        Object[] result = (Object[]) getSession().createSQLQuery("SELECT GETNODEID(), GEOREGION FROM SYSTEM.NODES WHERE ID=GETNODEID()")
                .uniqueResult();
        info.nodeId = ((Number) result[0]).intValue();
        info.regionName = result[1].toString();
        return info;
    }

    @Override
    public Currency getRegionCurrency(final String region) {
        @SuppressWarnings("unchecked")
        List<String> currencies = (List<String>) getSession()
                .createSQLQuery("SELECT DISTINCT Currency FROM APP_INSTANCE WHERE REGION=:REGION ORDER BY LAST_HEARTBEAT DESC")
                .setParameter("REGION", region).list();
        if (currencies.isEmpty()) {
            return null;
        }
        return Currency.valueOf(currencies.get(0));
    }

    protected void setStorefrontStatsParameters(SQLQuery query, Integer maxCustomerIdleTimeSec, Integer maxAgeSec) {
        Calendar now = Calendar.getInstance();

        // MIN_ACTIVE_TIME
        if (maxCustomerIdleTimeSec != null) {
            Calendar minActiveTime = (Calendar) now.clone();
            minActiveTime.add(Calendar.SECOND, -maxCustomerIdleTimeSec);
            query.setParameter("MIN_ACTIVE_TIME", minActiveTime);
        }

        // MIN_MODIFIED_TIME
        if (maxAgeSec != null) {
            Calendar minModifiedTime = (Calendar) now.clone();
            minModifiedTime.add(Calendar.SECOND, -maxAgeSec);
            query.setParameter("MIN_MODIFIED_TIME", minModifiedTime);
        }

        // MIN_HEARTBEAT_TIME
        Calendar minHeartbeatTime = (Calendar) now.clone();
        minHeartbeatTime.add(Calendar.SECOND, -StorefrontWebApp.MAX_HEARTBEAT_AGE_SEC);
        query.setParameter("MIN_HEARTBEAT_TIME", minHeartbeatTime);
    }

    protected static String toNumericString(Object o) {
        if (o != null) {
            String str = o.toString();
            if (str.length() > 0 && !str.equalsIgnoreCase("NaN")) {
                return str;
            }
        }
        return "0";
    }

    @Override
    protected void onTransactionComplete(String transactionName, long startTimeMs, boolean success) {
        if (transactionName == null) {
            return;
        }

        synchronized (transactionStatsMap) {
            TransactionStats stats = transactionStatsMap.get(transactionName);
            if (stats == null) {
                transactionStatsMap.put(transactionName, stats = new TransactionStats());
            }
            stats.incrementCount(transactionName, System.currentTimeMillis() - startTimeMs, success);
        }
    }
}
