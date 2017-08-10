<%-- Copyright (c) 2013-2017 NuoDB, Inc. --%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>

<t:page showHeader="false">
    <h1>CONTINUOUS AVAILABILITY</h1>
    <p>NuoDB’s architecture consists of a peer-to-peer network of transaction and storage process nodes. Create a redundant, resilient, continuously database by simply adding additional process nodes. If any of these nodes fails, overall database throughput will be impacted. However, NuoDB will continue to process transactions as long as it has at least one surviving transaction node and at least one storage node.</p>

    <img src="img/continuous-availability-diagram.png" style="width: 33%;"/>
    
    <p>When integrated with orchestration and management technologies, losing a node will trigger another process node to be spun up and automatically added back to the database. This allows the database to once again provide high levels of throughput for the application.</p>
    <h3>Try it yourself:</h3>
    <t:messages />
    <p>Add and shutdown processes to watch how the database provides continuous availability to the application.</p>
    <ol class="tour-steps">
        <li>In the TE Processes box, click on the &ldquo;up&rdquo; arrow to increase the number of NuoDB transaction nodes. Watch as throughput increases and latency decreases </li>
        <li>{add other steps and descriptions, pending information about what the user will do to experience CA}</li>
    </ol>

    <h3>Learn More:</h3>
    <ul class="tour-links">
        <li>See <a href="http://doc.nuodb.com/display/21V/Start+and+Stop+NuoDB+Services" target="_blank">NuoDB documentation</a> to learn how to increase the number of available hosts
        </li>
        <li>See <a href="control-panel-processes${qs}">Hosts &amp; Processes</a> in the Storefront Control Panel
        </li>
    </ul>

</t:page>
