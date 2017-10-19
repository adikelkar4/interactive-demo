<%-- Copyright (c) 2013-2017 NuoDB, Inc. --%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>

<t:page showHeader="false">
<t:messages></t:messages>
    <div class="header-logo"></div>
    <h1>ACTIVE-ACTIVE</h1>
    <p>
        NuoDB natively provides active-active capabilities, meaning that NuoDB can be deployed as a single logical database across multiple data centers while fully supporting concurrent application read/write activity in all locations. Unlike traditional SQL databases which require active-active add on technologies to provide this functionality, active-active capabilities are a part of NuoDB’s architectural design.
    </p>
    <div style="float:left;width:500px;padding-right:5%;padding-bottom:0%;padding-top:0%;">
    <img src="img/aa-a.png" style="width: 500%;"/>
    </div>
    <p>
        NuoDB can be deployed as a single logical database within a data center, across multiple data centers, or even across hybrid cloud environments while maintaining ACID guarantees and serving up data at in-memory speeds. 
    </p>
    <h3>About The Tour</h3>
    <p>
        In this Active-Active tour, evaluate how applications located in two AWS Availability Zones (AZs) can concurrently read and write to the database. Then, simulate a data center outage and see how the rest of the database (in the other datacenter) seamlessly continues operating with no downtime.
    </p>
    
    <h3>Try It Yourself</h3>
    <p>This tour is not available yet.</p>
    <a href="/vote-aa" style="color:#36af75;font-weight:700;">Vote for this tour next!</a>
    
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
