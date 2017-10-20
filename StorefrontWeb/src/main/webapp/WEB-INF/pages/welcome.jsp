<%-- Copyright (c) 2013-2017 NuoDB, Inc. --%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>

<t:page showHeader="false">
    <div class="header-logo"></div>
        <h1>NUODB ONLINE EXPERIENCE</h1>
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
        <h5> Welcome to the NuoDB Online Experience!</h5>
        <p>The NuoDB Online Experience allows you to quickly exercise and evaluate NuoDB’s unique capabilities. NuoDB is an SQL OLTP database that:</p>
        <ol>
            <li> Elastically scales out (and back in) on-demand to outperform traditional single-server databases and easily meet changing performance demands.</li>
            <li>Supports active-active workloads, which increases performance and server utilization while providing non-stop disaster recovery (DR) protection.</li>
            <li>Provides continuous availability for planned and unplanned outages within and across multiple data centers (DC).</li>
        </ol>
        <p>NuoDB natively provides these benefits without forcing you to give up familiar database benefits such as full ACID guarantees and SQL transaction support. We hope you enjoy your Online Experience!</p>

        <h5>When you are ready to dive deeper you can:</h5>
        <ul>
            <li>Download the Online Experience from <a href="https://github.com/nuodb/interactive-demo" style="color:#36af75;font-weight:700;" target="_blank">GitHub</a> to explore and deploy it in your own AWS environment</li>
        <li>Download the <a href="https://www.nuodb.com/product/evaluate-nuodb?utm_source=demo&utm_content=welcome" style="color:#36af75;font-weight:700;" target="_blank">NuoDB Community Edition</a> to evaluate NuoDB with your own application.</li>
        </ul>
        <p> <em><strong>[NOTE] </strong>This Online Experience environment is deployed and hosted specifically for you and will automatically become unavailable one hour after the Online Experience environment is deployed. If you need additional time to evaluate NuoDB using  the Online Experience, please contact <a href="mailto:sales@nuodb.com" target="_top" style="color:#36af75;font-weight:700;">sales@nuodb.com</a></em></p>

        <h2>Select a tour to get started</h2>
        <div class="home-tour-links" style="padding-bottom:10%;">
            <div class="home-page-btn">
                <a href="/tour-scale-out"><img src="img/scale-btn.png"/></a>
            </div>
            <div class="home-page-btn-text">
                <a href="/tour-scale-out" style="color:#282828;text-decoration:none;"><h5>Scale-Out Performance</h5>
                <p>Dynamically adjust both application  workload and database resources to see how NuoDB can dynamically scale to maintain high performance and low latencies.</p></a>
            </div>
            <div class="home-page-btn">
                <a href="/tour-database-comparison"><img src="img/db-comparison-btn-grey.png" /></a>
            </div>
            <div class="home-page-btn-text">    
                <h5 style="color:#98a0ad;">Database Comparison<br>[COMING SOON]</h5>
                <p style="color:#98a0ad;">Discover how  NuoDB scale out can out-perform a traditional single node database once the single node resources are saturated by the application workload.</p>
                <a href="/vote-dbc" style="color:#36af75;font-weight:700;">Vote for this tour next!</a>
            </div>
        </div>    
        <div class="home-tour-links">
            <div class="home-page-btn">
                <a href="/tour-active-active"><img src="img/active-active-btn.png" /></a>
            </div>
            <div class="home-page-btn-text">
                <h5 style="color:#98a0ad;">Active-Active<br>[COMING SOON]</h5>
                <p style="color:#98a0ad;">Scale your workload and NuoDB database across multiple AWS Availability Zones  to provide active-active capabilities.</p>
                <a href="/vote-aa" style="color:#36af75;font-weight:700;">Vote for this tour next!</a>
            </div>
            <div class="home-page-btn">
                <a href="/tour-continuous-availability"><img src="img/ca-btn-grey.png" /></a>
            </div>
            <div class="home-page-btn-text">                    
                <h5 style="color:#98a0ad;">Continuous Availability<br>[COMING SOON]</h5>
                <p style="color:#98a0ad;">Induce various types of failure modes and watch how NuoDB provides uninterrupted service to the application</p>
                <a href="/vote-ca" style="color:#36af75;font-weight:700;">Vote for this tour next!</a>
            </div>
        </div>
    </div>
</t:page>
