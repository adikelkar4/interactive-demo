<%-- Copyright (c) 2013-2017 NuoDB, Inc. --%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>

<t:page showHeader="false">
    <h1>ACTIVE-ACTIVE</h1>
    
    <p>Active-active capabilities are a natural result of NuoDB’s peer-to-peer distributed, two-layer architectural design. NuoDB can be deployed as a single logical database within a data center, across multiple data centers, or even across hybrid cloud environments while maintaining ACID guarantees and serving up data at in-memory speeds. Watch how applications located in both environments can read and write to the database. Then, simulate a data center outage and see how the rest of the database (in the other datacenter) seamlessly takes over the workload with no downtime.</p>

    <img src="img/aa-diagram.png" style="width: 60%;"/>

    <h3>Try it yourself:</h3>
    <t:messages />

    <ol class="tour-steps">
        <li>Lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum. Lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum. </li>
        <li>Lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum. Lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum. </li>
        <li>Lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum. Lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum. </li>
    </ol>
    
    <h3>Learn More:</h3>
    <ul class="tour-links">
        <li>See <a href="http://doc.nuodb.com/display/21V/Start+and+Stop+NuoDB+Services" target="_blank">NuoDB documentation</a> to learn how to increase the number of available hosts</li>
        <li>See <a href="control-panel-processes${qs}">Hosts &amp; Processes</a> in the Storefront Control Panel</li>
    </ul>

</t:page>
