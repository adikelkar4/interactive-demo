/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import com.nuodb.storefront.StorefrontTenantManager;
import com.nuodb.storefront.model.entity.Product;
import com.nuodb.storefront.service.IDataGeneratorService;
import com.nuodb.storefront.service.IStorefrontTenant;
import com.nuodb.storefront.util.NetworkUtil;

public class StorefrontWebApp implements ServletContextListener {
    private static final String CONTEXT_INIT_PARAM_PUBLIC_URL = "storefront.publicUrl";
    private static final String CONTEXT_INIT_PARAM_LAZY_LOAD = "storefront.lazyLoad";

    private static boolean s_initialized = false;
    private static String s_webAppUrlTemplate;
    private static String s_hostname;
	// Storefront properties
	public static final String IP_DETECT_URL = System.getProperty("storefront.ipDetectUrl", "http://checkip.amazonaws.com");
	public static final String DEFAULT_URL = System.getProperty("storefront.url", "{protocol}://{host}:{port}/{context}");
	public static final int DEFAULT_PORT = Integer.valueOf(System.getProperty("maven.tomcat.port", "9001"));
	public static final String TENANT_PARAM_NAME = "tenant";
	public static final String LOGGER_NAME_TENANT_SEP = ":";
	// Storefront timings
	public static final int HEARTBEAT_INTERVAL_SEC = 10;
	public static final int CPU_SAMPLING_INTERVAL_SEC = 1;
	public static final int MAX_HEARTBEAT_AGE_SEC = 20;
	public static final int PURGE_FREQUENCY_SEC = 60 * 30; // 30 min
	public static final int STOP_USERS_AFTER_IDLE_UI_SEC = 60 * 10; // 10 min
	public static final int MIN_INSTANCE_PURGE_AGE_SEC = 60 * 60; // 1 hour
	public static final int DEFAULT_SESSION_TIMEOUT_SEC = 60 * 20;// 20 min
	public static final int DEFAULT_ANALYTIC_MAX_AGE = 60 * 30;// 30 min
	public static final int BENCHMARK_DURATION_MS = 10000;
	public static final int SIMULATOR_STATS_DISPLAY_INTERVAL_MS = 5000;
	public static final int GC_CUMULATIVE_TIME_LOG_MS = 500; // every 0.5 sec of cumulative GC time logged
	// Database properties
	public static final String DB_NAME = System.getProperty("storefront.db.name");
	public static final String DB_USER = System.getProperty("storefront.db.user");
	public static final String DB_PASSWORD = System.getProperty("storefront.db.password");
	public static final String DB_OPTIONS = System.getProperty("storefront.db.options");
	public static final int DB_PING_TIMEOUT_SEC = Integer.valueOf(System.getProperty("storefront.db.pingTimeoutSec", "0")); // 0=disabled
	public static final int DB_MAX_INIT_WAIT_TIME_SEC = Integer.valueOf(System.getProperty("storefront.db.initWaitTimeSec", "5"));
	public static final String DB_DOMAIN_BROKER =System.getProperty("domain.broker", "localhost");
	public static final String DB_PROCESS_TAG = System.getProperty("storefront.db.processTag", "demo_${db.name}");
	// Database API properties
	public static final int DBAPI_READ_TIMEOUT_SEC = Integer.valueOf(System.getProperty("storefront.dbapi.readTimeoutSec", "10"));
	public static final int DBAPI_CONNECT_TIMEOUT_SEC = Integer.valueOf(System.getProperty("storefront.dbapi.connectTimeoutSec", "10"));
	public static final int DBAPI_MAX_UNAVAILABLE_RETRY_TIME_SEC = Integer.valueOf(System.getProperty("storefront.dbapi.unavailableRetryTimeSec", "3"));
	public static final String DBAPI_HOST = System.getProperty("storefront.dbapi.host");
	public static final int DBAPI_PORT = Integer.valueOf(System.getProperty("storefront.dbapi.port", "8888"));
	public static final String DBAPI_USERNAME = System.getProperty("storefront.dbapi.user", "domain");
	public static final String DBAPI_PASSWORD = System.getProperty("storefront.dbapi.password", "bird");
	// Other properties
	public static final int SQLEXPLORER_PORT = Integer.valueOf(System.getProperty("storefront.sqlexplorer.port", "9001"));
	// Default names
	public static final String DEFAULT_DB_NAME = "Storefront";
	public static final String DEFAULT_DB_HOST = "localhost";
	public static final String DEFAULT_REGION_NAME = "Unknown region";
	public static final String DEFAULT_TENANT_NAME = "Default";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        if (s_initialized) {
            // Context might be reinitialized due to code edits -- don't reinitialize hearbeat service, though
            return;
        }

        ServletContext context = sce.getServletContext();

        // Get external URL of this web app
        initWebAppUrl(context);

        // Initialize heartbeat service
        if (!isInitParameterTrue(CONTEXT_INIT_PARAM_LAZY_LOAD, context, false)) {
            StorefrontTenantManager.getDefaultTenant().startUp();
        }

        s_initialized = true;
    }

    protected static boolean isInitParameterTrue(String name, ServletContext context, boolean defaultValue) {
        String val = context.getInitParameter(name);
        if (StringUtils.isEmpty(val)) {
            return defaultValue;
        }
        return (val.equalsIgnoreCase("true") || val.equals("1"));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // Stop sending heartbeats
        for (IStorefrontTenant tenant : StorefrontTenantManager.getAllTenants()) {
            tenant.shutDown();
        }
    }

    public static void loadData(IDataGeneratorService svc) throws IOException {
	    InputStream stream = StorefrontWebApp.class.getClassLoader().getResourceAsStream("sample-products.json");
	    ObjectMapper mapper = new ObjectMapper();
	
	    // Read products from JSON file
	    List<Product> products = mapper.readValue(stream, new TypeReference<ArrayList<Product>>() {
	    });
	
	    // Load products into DB, and load generated views
	    try {
	        svc.generateProductReviews(100, products, 10);
	    } finally {
	        svc.close();
	    }
	}

	public static void removeData(IDataGeneratorService svc) throws IOException {
	    try {
	        svc.removeAll();
	    } finally {
	        svc.close();
	    }
	}

	public static void generateData(IDataGeneratorService svc) throws IOException {
	    try {
	        svc.generateAll(100, 5000, 2, 10);
	    } finally {
	        svc.close();
	    }
	}

	public static void updateWebAppUrl(HttpServletRequest req) {
        updateWebAppUrl(req.isSecure(), req.getHeader("HOST").split(":")[0], req.getServerPort(), req.getServletContext().getContextPath());
    }

    public static void initWebAppUrl(ServletContext context) {
        // Get URL from command line argument
        s_webAppUrlTemplate = StorefrontWebApp.DEFAULT_URL;
        if (StringUtils.isEmpty(s_webAppUrlTemplate)) {
            s_webAppUrlTemplate = context.getInitParameter(CONTEXT_INIT_PARAM_PUBLIC_URL);
            if (StringUtils.isEmpty(s_webAppUrlTemplate)) {
                s_webAppUrlTemplate = StorefrontWebApp.DEFAULT_URL;
            }
        }

        // Guess port
        int port = StorefrontWebApp.DEFAULT_PORT;

        // Get context path
        String contextPath = context.getContextPath();

        updateWebAppUrl(port == 443, NetworkUtil.getLocalIpAddress(), port, contextPath);
    }

    public static void updateWebAppUrl(boolean isSecure, String hostname, int port, String contextPath) {
        if (s_hostname != null && ("localhost".equals(hostname) || "127.0.0.1".equals(hostname) || "::1".equals(hostname))) {
            // Not helpful to update to a local address
            hostname = s_hostname;
        } else {
            s_hostname = hostname;
        }

        if (StringUtils.isEmpty(contextPath)) {
            contextPath = "";
        } else if (contextPath.startsWith("/")) {
            contextPath = contextPath.substring(1);
        }

        String url = s_webAppUrlTemplate
                .replace("{protocol}", isSecure ? "https" : "http")
                .replace("{host}", hostname)
                .replace("{port}", String.valueOf(port))
                .replace("{context}", contextPath);

        if (url.endsWith("/")) {
            // Don't want a trailing slash
            url = url.substring(0, url.length() - 1);
        }
        
        for (IStorefrontTenant tenant : StorefrontTenantManager.getAllTenants()) {
            tenant.getAppInstance().setUrl(url);            
        }        
    }
}
