<%-- Copyright (c) 2013-2017 NuoDB, Inc. --%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>

<t:page showHeader="false">
    <t:messages></t:messages>
    <div class="header-logo"></div>
    <h1>ABOUT THIS DEMO</h1>
    
    <h3>Introduction</h3>
        <p>Lorem Ipsum is simply dummy text of the printing and typesetting industry. Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, when an unknown printer took a galley of type and scrambled it to make a type specimen.</p>
    <img src="img/nuodb-architecture.png" style="width: 33%;"/>
    <div class="about-right">
        <h3>How It's Built</h3>
        <p>AWS autoscaling services - NuoDB seamlessly integrates with orchestration and cloud tools like AWS autoscaling.  Services monitors latency and will automatically adjust up or down number of transaction processes to accommodate increased or decreased workload.</p>
    </div>
    <h3>Build Your Own</h3>
        <p>Experience the demo in your own environment! Dive into the details of the application and NuoDB by downloading the code and following instructions in the Github repository.</p>
    
    <h3>Learn More:</h3>
        <a href="http://doc.nuodb.com/display/21V/Start+and+Stop+NuoDB+Services" target="_blank">
            <button class="button button1">Download the Code</button>
        </a>
        <a href="https://github.com/nuodb/interactive-demo" target="_blank">
            <button class="button button1">Demo Implementation Documentation</button>
        </a>

</t:page>
