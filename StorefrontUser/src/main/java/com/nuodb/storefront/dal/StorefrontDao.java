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
import java.util.concurrent.Callable;

import org.hibernate.Hibernate;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.type.BigDecimalType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.LongType;

import com.googlecode.genericdao.search.SearchResult;
import com.nuodb.storefront.StorefrontApp;
import com.nuodb.storefront.model.dto.Category;
import com.nuodb.storefront.model.dto.DbRegionInfo;
import com.nuodb.storefront.model.dto.ProductFilter;
import com.nuodb.storefront.model.dto.StorefrontStats;
import com.nuodb.storefront.model.dto.TransactionStats;
import com.nuodb.storefront.model.entity.IEntity;
import com.nuodb.storefront.model.entity.Product;
import com.nuodb.storefront.model.entity.PurchaseSelection;
import com.nuodb.storefront.model.type.Currency;
import com.nuodb.storefront.model.type.ProductSort;

/**
 * Data access object designed for storefront operations, built on top of a general-purpose DAO. The caller is responsible for wrapping DAO calls in
 * transactions, typically by using the {@link #runTransaction(Callable)} or {@link #runTransaction(Runnable)} method.
 */
public class StorefrontDao extends BaseDao implements IStorefrontDao {
    private final Map<String, TransactionStats> transactionStatsMap;

    public StorefrontDao(Map<String, TransactionStats> transactionStatsMap) {
        this.transactionStatsMap = transactionStatsMap;
    }

    public void initialize(IEntity entity) {
        Hibernate.initialize(entity);
    }

    
    public void evict(IEntity entity) {
        getSession().evict(entity);
    }

    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public SearchResult<Category> getCategories() {
        // This query got category usage counts, but was slower, and we currently don't need the counts for anything:
        // "select c, count(*) from Product p inner join p.categories c group by c order by c"

        List categories = getSession().createSQLQuery("SELECT DISTINCT CATEGORY, 0 FROM PRODUCT_CATEGORY ORDER BY CATEGORY").list();
        for (int i = categories.size() - 1; i >= 0; i--) {
            Object[] data = (Object[]) categories.get(i);
            categories.set(i, new Category((String) data[0], ((Number) data[1]).intValue()));
        }

        SearchResult result = new SearchResult<Category>();
        result.setResult(categories);
        result.setTotalCount(categories.size());
        return result;
    }

    @SuppressWarnings("unchecked")
    public SearchResult<Product> getProducts(ProductFilter filter) {
        Session session = getSession();

        SearchResult<Product> result = new SearchResult<Product>();
        result.setResult(buildProductQuery(filter, false).list());
        result.setTotalCount(((Number) buildProductQuery(filter, true).uniqueResult()).intValue());

        for (Product p : result.getResult()) {
            session.evict(p);
        }
        return result;
    }

    public StorefrontStats getStorefrontStats(int maxCustomerIdleTimeSec, Integer maxAgeSec) {
        final String ALL_STATS_QUERY = "SELECT"
                + " (SELECT COUNT(*) FROM PRODUCT) AS PRODUCT_COUNT,"
                + " (SELECT COUNT(*) FROM (SELECT DISTINCT CATEGORY FROM PRODUCT_CATEGORY) AS A) AS CATEGORY_COUNT,"
                + " (SELECT COUNT(*) FROM PRODUCT_REVIEW) AS PRODUCT_REVIEW_COUNT,"
                + " (SELECT COUNT(*) FROM CUSTOMER) AS CUSTOMER_COUNT,"
                + " (SELECT COUNT(*) FROM CUSTOMER WHERE DATE_LAST_ACTIVE >= :MIN_ACTIVE_TIME) AS ACTIVE_CUSTOMER_COUNT,"
                + " (SELECT COUNT(*) FROM CUSTOMER WHERE WORKLOAD IS NULL AND DATE_LAST_ACTIVE >= :MIN_ACTIVE_TIME) AS ACTIVE_WEB_CUSTOMER_COUNT,"
                + " (SELECT SUM(QUANTITY) FROM CART_SELECTION) AS CART_ITEM_COUNT,"
                + " (SELECT SUM(QUANTITY * UNIT_PRICE) FROM CART_SELECTION) AS CART_VALUE,"
                + " (SELECT COUNT(*) FROM PURCHASE) AS PURCHASE_COUNT,"
                + " (SELECT SUM(QUANTITY) FROM PURCHASE_SELECTION) AS PURCHASE_ITEM_COUNT,"
                + " (SELECT SUM(QUANTITY * UNIT_PRICE) FROM PURCHASE_SELECTION) AS PURCHASE_VALUE,"
                + " (SELECT MIN(DATE_STARTED) FROM APP_INSTANCE WHERE LAST_HEARTBEAT >= :MIN_HEARTBEAT_TIME) AS START_TIME"
                + " FROM DUAL;";

        final String RECENT_STATS_QUERY = "SELECT"
                + " (SELECT COUNT(*) FROM PRODUCT) AS PRODUCT_COUNT,"
                + " (SELECT COUNT(*) FROM (SELECT DISTINCT CATEGORY FROM PRODUCT_CATEGORY) AS A) AS CATEGORY_COUNT,"
                + " (SELECT COUNT(*) FROM PRODUCT_REVIEW WHERE DATE_ADDED >= :MIN_MODIFIED_TIME) AS PRODUCT_REVIEW_COUNT,"
                + " (SELECT COUNT(*) FROM CUSTOMER) AS CUSTOMER_COUNT,"
                + " (SELECT COUNT(*) FROM CUSTOMER WHERE DATE_LAST_ACTIVE >= :MIN_ACTIVE_TIME) AS ACTIVE_CUSTOMER_COUNT,"
                + " (SELECT COUNT(*) FROM CUSTOMER WHERE DATE_LAST_ACTIVE >= :MIN_ACTIVE_TIME AND WORKLOAD IS NULL) AS ACTIVE_WEB_CUSTOMER_COUNT,"
                + " (SELECT SUM(QUANTITY) FROM CART_SELECTION WHERE DATE_MODIFIED >= :MIN_MODIFIED_TIME) AS CART_ITEM_COUNT,"
                + " (SELECT SUM(QUANTITY * UNIT_PRICE) FROM CART_SELECTION WHERE DATE_MODIFIED >= :MIN_MODIFIED_TIME) AS CART_VALUE,"
                + " (SELECT COUNT(*) FROM PURCHASE WHERE DATE_PURCHASED >= :MIN_MODIFIED_TIME) AS PURCHASE_COUNT,"
                + " (SELECT SUM(QUANTITY) FROM PURCHASE_SELECTION WHERE DATE_MODIFIED >= :MIN_MODIFIED_TIME) AS PURCHASE_ITEM_COUNT,"
                + " (SELECT SUM(QUANTITY * UNIT_PRICE) FROM PURCHASE_SELECTION WHERE DATE_MODIFIED >= :MIN_MODIFIED_TIME) AS PURCHASE_VALUE,"
                + " (SELECT MIN(DATE_STARTED) FROM APP_INSTANCE WHERE LAST_HEARTBEAT >= :MIN_HEARTBEAT_TIME) AS START_TIME"
                + " FROM DUAL;";

        // Run query
        SQLQuery query = getSession().createSQLQuery((maxAgeSec == null) ? ALL_STATS_QUERY : RECENT_STATS_QUERY);
        query.addScalar("PRODUCT_COUNT", IntegerType.INSTANCE);
        query.addScalar("CATEGORY_COUNT", IntegerType.INSTANCE);
        query.addScalar("CART_VALUE", IntegerType.INSTANCE);
        query.addScalar("PRODUCT_REVIEW_COUNT", IntegerType.INSTANCE);
        query.addScalar("CUSTOMER_COUNT", IntegerType.INSTANCE);
        query.addScalar("ACTIVE_CUSTOMER_COUNT", IntegerType.INSTANCE);
        query.addScalar("ACTIVE_WEB_CUSTOMER_COUNT", IntegerType.INSTANCE);
        query.addScalar("CART_ITEM_COUNT", IntegerType.INSTANCE);
        query.addScalar("CART_VALUE", BigDecimalType.INSTANCE);
        query.addScalar("PURCHASE_COUNT", IntegerType.INSTANCE);
        query.addScalar("PURCHASE_ITEM_COUNT", IntegerType.INSTANCE);
        query.addScalar("PURCHASE_VALUE", IntegerType.INSTANCE);
        query.addScalar("START_TIME", LongType.INSTANCE);
        setStorefrontStatsParameters(query, maxCustomerIdleTimeSec, maxAgeSec);
        Object[] results = (Object[]) query.uniqueResult();

        // Fill stats
        StorefrontStats stats = new StorefrontStats();
        stats.setProductCount(getIntValue(results[0]));
        stats.setCategoryCount(getIntValue(results[1]));
        stats.setProductReviewCount(getIntValue(results[2]));
        stats.setCustomerCount(getIntValue(results[3]));
        stats.setActiveCustomerCount(getIntValue(results[4]));
        stats.setActiveWebCustomerCount(getIntValue(results[5]));
        stats.setCartItemCount(getIntValue(results[6]));
        stats.setCartValue(getBigDecimalValue(results[7]));
        stats.setPurchaseCount(getIntValue(results[8]));
        stats.setPurchaseItemCount(getIntValue(results[9]));
        stats.setPurchaseValue(getBigDecimalValue(results[10]));

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(getLongValue(results[11]));
        stats.setDateStarted(cal);

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

    public int deleteDeadAppInstances(Calendar maxLastHeartbeat) {
        SQLQuery query = getSession().createSQLQuery("DELETE FROM APP_INSTANCE WHERE LAST_HEARTBEAT <= :MAX_LAST_HEARTBEAT");
        query.setParameter("MAX_LAST_HEARTBEAT", maxLastHeartbeat);
        return query.executeUpdate();
    }

    public int getActiveAppInstanceCount(Calendar idleThreshold) {
        SQLQuery query = getSession().createSQLQuery(
                "SELECT COUNT(*) FROM APP_INSTANCE WHERE" +
                        " (STOP_USERS_WHEN_IDLE = 0 AND LAST_HEARTBEAT >= :MIN_HEARTBEAT_TIME)" +
                        " OR LAST_API_ACTIVITY > :IDLE_THRESHOLD");
        setStorefrontStatsParameters(query, null, null);
        query.setParameter("IDLE_THRESHOLD", idleThreshold);
        return ((Number) query.uniqueResult()).intValue();
    }

    public DbRegionInfo getCurrentDbNodeRegion() {
        DbRegionInfo info = new DbRegionInfo();
        Object[] result = (Object[]) getSession().createSQLQuery("SELECT GETNODEID(), GEOREGION FROM SYSTEM.NODES WHERE ID=GETNODEID()")
                .uniqueResult();
        info.nodeId = ((Number) result[0]).intValue();
        info.regionName = result[1].toString();
        return info;
    }

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

    public void incrementPurchaseCounts(final List<PurchaseSelection> selections) {
        try {
            ConnectionProvider connProvider = getSessionFactory().getSessionFactoryOptions().getServiceRegistry()
                    .getService(ConnectionProvider.class);
            Connection connection = connProvider.getConnection();
            try {
                connection.setAutoCommit(true);
                Statement stmt = connection.createStatement();

                // Batch updates for products with the same quantity increment
                Set<Integer> seenQuantities = new HashSet<Integer>();
                int numSelections = selections.size();
                for (int i = 0; i < numSelections; i++) {
                    PurchaseSelection selection = selections.get(i);
                    int quantity = selection.getQuantity();
                    if (seenQuantities.add(quantity)) {
                        String productIdList = String.valueOf(selection.getProduct().getId());
                        for (int j = i + 1; j < numSelections; j++) {
                            PurchaseSelection selection2 = selections.get(j);
                            if (selection2.getQuantity() == quantity) {
                                productIdList += "," + selection2.getProduct().getId();
                            }
                        }
                        
                        stmt.executeUpdate("UPDATE PRODUCT SET PURCHASE_COUNT = PURCHASE_COUNT + " + quantity + " WHERE ID IN (" + productIdList + ")");
                    }
                }
            } finally {
                connProvider.closeConnection(connection);
            }
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        minHeartbeatTime.add(Calendar.SECOND, -StorefrontApp.MAX_HEARTBEAT_AGE_SEC);
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

    protected SQLQuery buildProductQuery(ProductFilter filter, boolean countOnly) {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<String, Object>();

        if (countOnly) {
            sql.append("SELECT COUNT(*) FROM PRODUCT");
        } else {
            sql.append("SELECT * FROM PRODUCT");
        }

        // Set match text
        String matchText = filter.getMatchText();
        if (matchText != null && !matchText.isEmpty()) {
            matchText = "%" + matchText.trim().toLowerCase() + "%";
            sql.append(" AND (LOWER(NAME) LIKE :MATCH_TEXT OR LOWER(DESCRIPTION) LIKE :MATCH_TEXT)");
            params.put("MATCH_TEXT", matchText);
        }

        // Set categories
        Collection<String> categories = filter.getCategories();
        if (categories != null && !categories.isEmpty()) {
            StringBuilder categoryParamList = new StringBuilder();
            int categoryIdx = 0;
            for (String category : categories) {
                if (categoryIdx > 0) {
                    categoryParamList.append(", ");
                }
                String catParamName = "cat" + ++categoryIdx;
                params.put(catParamName, category);
                categoryParamList.append(":" + catParamName);
            }
            sql.append(" AND ID IN (SELECT PRODUCT_ID FROM PRODUCT_CATEGORY WHERE CATEGORY IN (" + categoryParamList + "))");
        }

        // Set sort
        ProductSort sort = filter.getSort();
        if (sort != null && !countOnly) {
            switch (sort) {
                case AVG_CUSTOMER_REVIEW:
                    sql.append(" ORDER BY COALESCE(RATING, -1) DESC, REVIEW_COUNT DESC");
                    break;

                case DATE_CREATED:
                    sql.append(" ORDER BY DATE_ADDED DESC");
                    break;

                case NEW_AND_POPULAR:
                    sql.append(" ORDER BY PURCHASE_COUNT DESC, DATE_ADDED DESC");
                    break;

                case PRICE_HIGH_TO_LOW:
                    sql.append(" ORDER BY UNIT_PRICE DESC");
                    break;

                case PRICE_LOW_TO_HIGH:
                    sql.append(" ORDER BY UNIT_PRICE");
                    break;

                case RELEVANCE:
                    if (matchText != null && !matchText.isEmpty()) {
                        sql.append(" ORDER BY CASE WHEN LOWER(NAME) LIKE :MATCH_TEXT THEN 1 ELSE 0 END DESC, NAME, DATE_ADDED DESC");
                    } else {
                        sql.append(" ORDER BY NAME, DATE_ADDED DESC");
                    }
                    break;

                default:
                    sql.append(" ORDER BY ID");
                    break;
            }
        }

        // Replace first "AND" with "WHERE"
        int andIdx = sql.indexOf("AND");
        if (andIdx > 0) {
            sql.replace(andIdx, andIdx + "AND".length(), "WHERE");
        }

        // Build SQL
        SQLQuery query = getSession().createSQLQuery(sql.toString());
        if (!countOnly) {
            query.addEntity(Product.class);
        }
        for (Map.Entry<String, Object> param : params.entrySet()) {
            query.setParameter(param.getKey(), param.getValue());
        }

        // Set pagination params (limit and offset)
        if (!countOnly) {
            Integer pageSize = filter.getPageSize();
            if (pageSize != null && pageSize > 0) {
                Integer page = filter.getPage();
                if (page != null) {
                    query.setFirstResult(pageSize * (page - 1));
                }
                query.setMaxResults(pageSize);
            }
        }

        return query;
    }

    @Override
    protected void onTransactionComplete(String transactionName, long duration, boolean success) {
        if (transactionName == null) {
            return;
        }

        synchronized (transactionStatsMap) {
            TransactionStats stats = transactionStatsMap.get(transactionName);
            if (stats == null) {
                transactionStatsMap.put(transactionName, stats = new TransactionStats());
            }
            stats.incrementCount(transactionName, duration, success);
        }
    }
}
