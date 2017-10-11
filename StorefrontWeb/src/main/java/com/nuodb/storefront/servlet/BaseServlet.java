/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.nuodb.storefront.StorefrontTenantManager;
import com.nuodb.storefront.api.ProcessesApi;
import com.nuodb.storefront.model.dto.Message;
import com.nuodb.storefront.model.dto.PageConfig;
import com.nuodb.storefront.model.entity.AppInstance;
import com.nuodb.storefront.model.entity.Customer;
import com.nuodb.storefront.model.type.MessageSeverity;
import com.nuodb.storefront.service.IDbApi;
import com.nuodb.storefront.service.IStorefrontTenant;

public abstract class BaseServlet extends HttpServlet {
    public static final String ATTR_PAGE_CONFIG = "pageConfig";
    public static final String SESSION_PRODUCT_FILTER = "productFilter";
    public static final String ATTR_TENANT = "tenant";
    public static final String ATTR_BASE_HREF = "baseHref";

    private static final String ATTR_CUSTOMER = "customer";
    private static final String SESSION_MESSAGES = "messages";
    private static final long serialVersionUID = 1452096145544476070L;
    protected static Object s_schemaUpdateLock = new Object();

    protected BaseServlet() {
    }

    public static IStorefrontTenant getTenant(HttpServletRequest req) {
        return StorefrontTenantManager.getTenant(req);
    }

    public static IDbApi getDbApi(HttpServletRequest req) {
        return getTenant(req).getDbApi();
    }

    public static Message addErrorMessage(HttpServletRequest req, Exception e) {
        Message msg = new Message(e);
        getMessages(req).add(msg);
        return msg;
    }

    public static Message addMessage(HttpServletRequest req, MessageSeverity severity, String message, String... buttons) {
        Message msg = new Message(severity, message, buttons);
        getMessages(req).add(msg);
        return msg;
    }

    public static List<Message> getMessages(HttpServletRequest req) {
        HttpSession session = req.getSession();

        @SuppressWarnings("unchecked")
        List<Message> messages = (List<Message>)session.getAttribute(SESSION_MESSAGES);
        if (messages == null) {
            messages = new ArrayList<Message>();
            session.setAttribute(SESSION_MESSAGES, messages);
        }

        return messages;
    }

    public static int getMessageCount(List<Message> messages, MessageSeverity severity) {
        int count = 0;
        if (messages != null) {
            for (Message msg : messages) {
                if (msg.getSeverity() == severity) {
                    count++;
                }
            }
        }
        return count;
    }

    protected static void showPage(HttpServletRequest req, HttpServletResponse resp, String pageTitle, String pageName, Object pageData,
            Customer customer) throws ServletException, IOException {

    	if (!ProcessesApi.initializeTourInfrastructure(pageName, req)) {
    		getLogger(req, BaseServlet.class).warn("Tour was not initialized");
    	}
        StorefrontWebApp.updateWebAppUrl(req);
        IStorefrontTenant tenant = getTenant(req);
        AppInstance appInstance = tenant.getAppInstance();

        // Build full page title
        String storeName = appInstance.getName() + " - " + PageConfig.APP_NAME;
        if (pageTitle == null || pageTitle.isEmpty()) {
            pageTitle = storeName;
        } else {
            pageTitle = pageTitle + " - " + storeName;
        }
        if (!StorefrontTenantManager.isDefaultTenant(tenant)) {
            pageTitle += " [" + appInstance.getTenantName() + "]";
        }

        PageConfig initData = new PageConfig(pageTitle, pageName, pageData, customer, getMessages(req), Arrays.asList(appInstance) );
        req.setAttribute(ATTR_PAGE_CONFIG, initData);
        req.getSession().removeAttribute(SESSION_MESSAGES);

        // Render JSP page
        getLogger(req, BaseServlet.class).info("Servicing \"" + req.getMethod() + " " + req.getRequestURI() + "\" with \"" + pageName + ".jsp\" for customer "
                + ((customer == null) ? null : customer.getId()) + " from " + req.getRemoteAddr());
        req.getRequestDispatcher("/WEB-INF/pages/" + pageName + ".jsp").forward(req, resp);
    }

    protected static void showCriticalErrorPage(HttpServletRequest req, HttpServletResponse resp, Exception ex) throws ServletException, IOException {
        getMessages(req).clear();
        addErrorMessage(req, ex);

        Customer customer = (Customer)req.getAttribute(ATTR_CUSTOMER);
        showPage(req, resp, "Storefront Problem", "error", null, (customer == null) ? new Customer() : customer);

        getLogger(req, BaseServlet.class).warn("Servlet handled critical error", ex);
    }
    
    protected static Logger getLogger(HttpServletRequest req, Class<?> clazz) {
        return StorefrontTenantManager.getTenant(req).getLogger(clazz);
    }
    
}
