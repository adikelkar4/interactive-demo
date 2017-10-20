<%-- Copyright (c) 2013-2017 NuoDB, Inc. --%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>

<t:page showHeader="false">
<t:messages></t:messages>
    <div class="header-logo"></div>
    <h1>DATABASE COMPARISON</h1>
    <p>
        In the <a href="/tour-scale-out" style="color:#36af75;font-weight:700;">Scale-Out Performance tour</a>, you can see how NuoDB easily accommodates increases in application workloads by simply scaling out with additional nodes. In contrast, traditional databases must be migrated to a larger server with more resources once the original deployment resources are fully utilized.
    </p>
    <div style="float:left;width:500px;padding-right:5%;padding-bottom:0%;padding-top:0%;">
        <img src="img/db-a.png" style="width: 500px;"/>
    </div>
    <h3>About The Tour</h3>
    <p>
        In this Database Comparison tour, you will compare how NuoDB can scale-out to address additional application workload demands compared to a traditional database deployed on a single server where the application workload cannot be increased.  
    </p>
    <br><br><br><br>
    <h3>Try It Yourself</h3>
    <p>This tour is not available yet.</p>
    <a href="/vote-dbc" style="color:#36af75;font-weight:700;">Vote for this tour next!</a>
    
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
