/* Copyright (c) 2013-2015 NuoDB, Inc. */

package com.nuodb.storefront.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import com.nuodb.storefront.model.dto.StorefrontStats;
import com.nuodb.storefront.model.entity.Customer;
import com.nuodb.storefront.model.type.MessageSeverity;
import com.nuodb.storefront.service.IDataGeneratorService;

public class ControlPanelProductsServlet extends BaseServlet {
    private static final long serialVersionUID = -1224032390706203080L;

    /**
     * GET: Shows the control panel screen, including the list of simulated workloads.
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Map<String, Object> pageData = new HashMap<String, Object>();
            showPage(req, resp, "Control Panel", "control-panel-products", pageData, new Customer());
        } catch (Exception ex) {
            showCriticalErrorPage(req, resp, ex);
        }
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
        IDataGeneratorService dataGen = getTenant(req).createDataGeneratorService();
        Logger logger = getLogger(req,  getClass());
        if (btnAction.contains("load")) {
            StorefrontWebApp.loadData(dataGen);
            addMessage(req, MessageSeverity.INFO, "Product data loaded successfully.");
            logger.info("Product data loaded");
        } else if (btnAction.contains("generate")) {
            StorefrontWebApp.generateData(dataGen);
            addMessage(req, MessageSeverity.INFO, "Product data generated successfully.");
            logger.info("Product data generated");
        } else if (btnAction.contains("remove")) {
            // Now remove all data
            try {
                StorefrontWebApp.removeData(dataGen);
                logger.info("Product data removed");
            } catch (Exception e) {
                logger.error("Unable to remove product data", e);
            }
        }
    }
}
