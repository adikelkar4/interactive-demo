/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nuodb.storefront.exception.ApiUnavailableException;
import com.nuodb.storefront.model.dto.ConnInfo;
import com.nuodb.storefront.model.dto.DbConnInfo;
import com.nuodb.storefront.model.entity.Customer;
import com.nuodb.storefront.service.IStorefrontTenant;

public class WelcomeServlet extends BaseServlet {
    private static final long serialVersionUID = 4369262156023258885L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> pageData = new HashMap<String, Object>();

        showPage(req, resp, "Welcome", "welcome", pageData, new Customer());
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String btnAction = req.getParameter("btn-msg");
            if (btnAction != null) {
                btnAction = btnAction.toLowerCase();
                doPostAction(req, resp, btnAction);
            }
        } catch (Exception ex) {
            getLogger(req, getClass()).error("Post failed", ex);
            addErrorMessage(req, ex);
        }

        doGet(req, resp);
    }

    protected void doPostAction(HttpServletRequest req, HttpServletResponse resp, String btnAction) throws IOException {
        IStorefrontTenant tenant = getTenant(req);

        if (btnAction.equals("api")) {
            ConnInfo apiConnInfo = new ConnInfo();
            apiConnInfo.setUrl(req.getParameter("api-url"));
            apiConnInfo.setUsername(req.getParameter("api-username"));
            apiConnInfo.setPassword(req.getParameter("api-password"));
            tenant.setApiConnInfo(apiConnInfo);

            // Wait until the API is connected to the domain
            for (int secondsWaited = 0; secondsWaited < StorefrontWebApp.DBAPI_MAX_UNAVAILABLE_RETRY_TIME_SEC; secondsWaited++) {
                try {
                    tenant.getDbApi().testConnection();
                    break;
                } catch (ApiUnavailableException e) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            }
        } else if (btnAction.equals("db")) {
            DbConnInfo connInfo = new DbConnInfo();
            connInfo.setUrl(req.getParameter("url"));
            connInfo.setUsername(req.getParameter("username"));
            connInfo.setPassword(req.getParameter("password"));
            tenant.setDbConnInfo(connInfo);
        }
    }
}
