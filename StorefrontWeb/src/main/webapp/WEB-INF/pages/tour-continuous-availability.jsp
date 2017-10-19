<%-- Copyright (c) 2013-2017 NuoDB, Inc. --%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>

<t:page showHeader="false">
<t:messages></t:messages>
    <div class="header-logo"></div>
    <h1>CONTINUOUS AVAILABILITY</h1>
    <p>
        NuoDB provides continuous availability within and across data centers for both planned and unplanned failures. Since NuoDB is a peer-to-peer distributed architecture, with no single point of failure and with built-in data replication, the loss of any server or process does not impact application availability. As seen in the <a href="/tour-active-active" style="color:#36af75;font-weight:700;">Active-Active tour</a>, this also includes protection against data center failures with no outages. In addition, NuoDB does not require any offline database maintenance tasks including upgrades. The database can be upgraded in a rolling fashion.
    </p>
    <img src="img/ca-a.png" style="width: 100%;"/>
    <h3>About The Tour</h3>
    <p>
        Use this Continuous Availability tour to simulate various types of failures and watch how NuoDB provides continuous availability database service to the application.
    </p>
    
    <h3>Try It Yourself</h3>
    <p>This tour is not available yet.</p>
    <a href="/vote-ca" style="color:#36af75;font-weight:700;">Vote for this tour next!</a>
    
    <h3>Next Steps</h3>
    <ul class="tour-links">
        <li>Download the Online Experience from GitHub to explore and deploy it in your own AWS environment</li>
        <li>Download the NuoDB Community Edition to evaluate NuoDB with your own application.</li>
    </ul>
    <a href="hhttps://github.com/nuodb/interactive-demo" target="_blank">
        <button class="button button1">Download the Code</button>
    </a>
    <a href="https://www.nuodb.com/product/evaluate-nuodb?utm_source=demo&utm_content=so" target="_blank">
        <button class="button button1">Download Community Edition</button>
    </a>

</t:page>
