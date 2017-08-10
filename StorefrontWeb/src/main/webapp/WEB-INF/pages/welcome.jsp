<%-- Copyright (c) 2013-2017 NuoDB, Inc. --%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>

<t:page showHeader="false">
    <h1>NUODB INTERACTIVE DEMO</h1>
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
        <p> Welcome to the NuoDB Interactive Online Demo!  In this demo, you control the applications to learn how NuoDB reacts to various scenarios. <br><br>For example, increase the workload and watch NuoDB automatically scale out to maintain high performance.  Simulate a datacenter failure and experience how NuoDB provides continuous availability to the application.  Learn how NuoDB compares to a traditional, monolithic database. <br><br> We hope you enjoy this interactive demo!  When you’ve finished exploring and are ready to dive deeper, build and test the demo in your own environment or try out our free Community Edition.</p>

        <h2>Select a tour to get started</h2>
        <div class="home-tour-links">
            <div class="home-page-btn">
                <a href="/tour-scale-out"><img src="img/scale-btn.png"/></a>
            </div>
            <div class="home-page-btn-text">
                <h5>Scale-Out Performance</h5>
                <p>Increase the workload on the database and watch the effects on performance as NuoDB scales out.</p>
            </div>
            <div class="home-page-btn">
                <a href="/tour-database-comparison"><img src="img/db-comparison-btn.png" /></a>
            </div>
            <div class="home-page-btn-text">    
                <h5 style="color:#98a0ad;">Database Comparison [COMING SOON]</h5>
                <p style="color:#98a0ad;">Compare how NuoDB performs and scales out compared to mySQL, PostgreSQL and, and Microsoft SQL Server.</p>
            </div>
        </div>    
        <div class="home-tour-links">
            <div class="home-page-btn">
                <a href="/tour-active-active"><img src="img/active-active-btn.png" /></a>
            </div>
            <div class="home-page-btn-text">
                <h5 style="color:#98a0ad;">Active-Active [COMING SOON]</h5>
                <p style="color:#98a0ad;">Watch how NuoDB provides active-active capabilities within and across multiple deployment environments without needing any additional technology or configuration.</p>
            </div>
            <div class="home-page-btn">
                <a href="/tour-continuous-availability"><img src="img/ca-btn.png" /></a>
            </div>
            <div class="home-page-btn-text">                    
                <h5 style="color:#98a0ad;">Continuous Availability [COMING SOON]</h5>
                <p style="color:#98a0ad;">Deploy NuoDB across multiple environments, simulate an outage scenario, and watch how NuoDB continues to provide uninterrupted service to the application.</p>
            </div>
        </div>
    </div>
</t:page>
