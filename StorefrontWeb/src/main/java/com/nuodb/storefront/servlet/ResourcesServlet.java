/* Copyright (c) 2013-2017 NuoDB, Inc. */

package com.nuodb.storefront.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nuodb.storefront.model.entity.Customer;

public class ResourcesServlet extends BaseServlet {
    private static final long serialVersionUID = 8555464129927540059L;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        showPage(req, resp, "Resources", "resources", null, new Customer());
    }
}
