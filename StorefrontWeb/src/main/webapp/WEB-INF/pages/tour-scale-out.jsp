<%-- Copyright (c) 2013-2017 NuoDB, Inc. --%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>

<t:page showHeader="false">
    <h1>SCALE-OUT PERFORMANCE</h1>
    <p>NuoDB is designed to easily scale out and back in to maintain performance throughout dynamically changing workloads.  Simply add transaction engine process nodes to boost transaction performance of the database.  Scale back transaction nodes to conserve utilization and resources when workload demands are not as high.<br><br>With NuoDB, it’s straightforward to integrate with orchestration and management tooling, such as Amazon’s autoscaling services {what’s the name of this?}, Kurbernetes, Mesos, Nomad, Docker Swarm, via NuoDB’s RESTful services API.</p>
    <div class="row-fluid tour-figure tall">
        <div class="span2">
            <h3>When user workload increases...</h3>
            <div class="thumbnail">
                <div class="caption">Workload</div>
                <img src="img/tour-up.png" height="80" />
            </div>
        </div>
        <div class="span1"></div>
        <div class="span2">
            <h3><br />...scale out by adding hosts</h3>
            <div class="thumbnail">
                <div class="caption">Hosts</div>
                <img src="img/tour-up.png" height="100" />
            </div>        
        </div>
        <div class="span1"></div>
        <div class="span2">
            <h3><br />Throughput increases</h3>
            <div class="thumbnail">
                <div class="caption">Throughput</div>
                <img src="img/tour-up.png" height="100" />
            </div>        
        </div>
        <div class="span2">
            <h3><br />...and latency decreases</h3>
            <div class="thumbnail">
                <div class="caption text-right">Latency</div>
                <img src="img/tour-down.png" height="100" />
            </div>        
        </div>
    </div>
    <h3>Try it yourself:</h3>
    <t:messages />
    <p>Increase and decrease the workload burden that the application places on the database and watch AWS and NuoDB automatically adjust to keep throughput high and latencies low.</p>
    <ol class="tour-steps">
        <li>In the Workload box, click on the &ldquo;up&rdquo; arrow to increase transaction requests to the database. Watch as the latency increases - a sign that the system is struggling to quickly complete requests.</li>
        <li>In the Processes box, click on the &ldquo;up&rdquo; arrow to add a new transaction process.</li>
        <li>When the new transaction engines come online, the database is able to process higher volumes of transaction requests.  This is reflected in higher volumes of throughput.</li>
        <li>With the new transactions engines up and running, the database can also process transactions more quickly, improving response time to the application. Fast response times (indicated by low latencies) is one metric used to judge high quality user experience.</li>
    </ol>
    
    <h3>To learn more:</h3>
    <ul class="tour-links">
        <li>See <a href="http://doc.nuodb.com/display/21V/Start+and+Stop+NuoDB+Services" target="_blank">NuoDB documentation</a> to learn how to increase the number of available hosts</li>
        <li>See <a href="control-panel-processes${qs}">Hosts &amp; Processes</a> in the Storefront Control Panel</li>
    </ul>

</t:page>
