<%-- Copyright (c) 2013-2015 NuoDB, Inc. --%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>

<t:page showHeader="false">
    <h1>NuoDB INTERACTIVE DEMO</h1>
    <t:messages />
    <div class="alert alert-block alert-info hide" id="api-box">
        <p>Before you can use this demo, you must specify domain credentials to connect to NuoDB.<br> &nbsp;</p>
        
        <form class="form-horizontal" method="post">
            <div class="control-group">
                <label class="control-label" for="api-username">Domain username:</label>
                <div class="controls">
                    <input class="input-xxlarge" type="text" id="api-username" name="api-username" placeholder="Username">
                </div>
            </div>
            <div class="control-group">
                <label class="control-label" for="api-password">Domain password:</label>
                <div class="controls">
                    <input class="input-xxlarge" type="text" id="api-password" name="api-password" placeholder="Password">
                </div>
            </div>
            <div class="control-group">
                <label class="control-label" for="api-url">API URL:</label>
                <div class="controls">
                    <input class="input-xxlarge" type="text" id="api-url" name="api-url" placeholder="URL">
                </div>
            </div>
            <div class="control-group">
                <div class="controls">
                    <button class="btn btn-info" type="submit" value="Connect to API">Connect to API</button>
                </div>
            </div>
            <input type="hidden" name="btn-msg" value="api" />
        </form>
    </div>

    <div class="alert alert-block alert-info hide" id="create-db-box">
        <p>The Storefront database does not exist yet.  Use this form to create it.<br> &nbsp;</p>
        
        <form class="form-horizontal" method="post">
            <div class="control-group">
                <label class="control-label" for="username">Database username:</label>
                <div class="controls">
                    <input class="input-xxlarge" type="text" id="username" name="username" placeholder="Username">
                </div>
            </div>
            <div class="control-group">
                <label class="control-label" for="password">Database password:</label>
                <div class="controls">
                    <input class="input-xxlarge" type="text" id="password" name="password" placeholder="Password">
                </div>
            </div>
            <div class="control-group">
                <label class="control-label" for="url">Broker URL:</label>
                <div class="controls">
                    <textarea class="input-xxlarge no-resize-x" id="url" name="url" placeholder="URL" rows="4"></textarea>
                    <p><small>Tip: You may change the database name by editing this URL.  You may also specify multiple brokers for failover support.<br />
                        Syntax: <code> jdbc:com.nuodb://{broker1}:{port1},{broker2}:{port2},..,{brokerN}:{portN}/{db-name}?{params}</code></small></p>
                </div>
            </div>
            <div class="control-group">
                <div class="controls">
                    <button class="btn btn-info" type="submit" value="Create database">Create database</button>
                </div>
            </div>
            <input type="hidden" name="btn-msg" value="db" />
        </form>
    </div>

    <div id="welcome">
        <div class="row">
            <h2>Select a tour to get started</h2>
            <div class="home-page-btn">
                <a href=""><img src="img/scale-btn.png"/></a>
                <h5 style="text-align: center;">Scale-Out Performance</h5>
                <p style="text-align: center;">Brief description of what the scale-out demo is</p>
            </div>
            <div class="home-page-btn">
                <a href=""><img src="img/db-comparison-btn.png" /></a>
                <h5 style="text-align: center;">[COMING SOON]</h5>
            </div>
            <div class="home-page-btn">
                <a href=""><img src="img/active-active-btn.png" /></a>
                <h5 style="text-align: center;">[COMING SOON]</h5>
            </div>
             <div class="home-page-btn">
                <a href=""><img src="img/ca-btn.png" /></a>
                <h5 style="text-align: center;">[COMING SOON]</h5>
            </div>
        </div>
    </div>
</t:page>
