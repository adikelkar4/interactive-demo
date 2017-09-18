<%-- Copyright (c) 2013-2017 NuoDB, Inc. --%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>

<t:page showHeader="false">
<t:messages></t:messages>
    <div class="header-logo"></div>
    <h1>DATABASE COMPARISON</h1>
    <p>Traditional relational databases are monolithic in nature, combine query processing and storage as a single service, and were designed for client-server systems.  While still maintaining full SQL data access, ACID transactions, and a relational model, NuoDB was designed to meet the modern performance requirements of today’s applications.  Learn how NuoDB performs compared to popular databases that were built on traditional, monolithic design principles - MySQL and Amazon Aurora.</p>
    
    <img src="img/db-comparison-diagram.png" style="width: 50%;"/>


    <h3>Try it yourself:</h3>
    <t:messages />
    <p>Lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum. Lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum. </p>
    <ol class="tour-steps">
        <li>{this will change depending on what we’re showing and how we’re showing it}</li>
        <li>{this will change depending on what we’re showing and how we’re showing it}</li>
        <li>{this will change depending on what we’re showing and how we’re showing it}</li>
    </ol>
    
    <h3>To learn more:</h3>
    <ul class="tour-links">
        <li>See <a href="http://doc.nuodb.com/display/21V/Start+and+Stop+NuoDB+Services" target="_blank">NuoDB documentation</a> to learn how to increase the number of available hosts</li>
        <li>See <a href="control-panel-processes${qs}">Hosts &amp; Processes</a> in the Storefront Control Panel</li>
    </ul>


</t:page>
