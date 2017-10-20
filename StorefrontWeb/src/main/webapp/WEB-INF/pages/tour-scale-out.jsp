<%-- Copyright (c) 2013-2017 NuoDB, Inc. --%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>

<t:page showHeader="false">
    <t:messages></t:messages>
    <div class="header-logo"></div>
    <h1>SCALE-OUT PERFORMANCE</h1>
    <p>
        Traditional relational databases are monolithic processes, which combine query processing and storage management in a single process. These solutions were designed for client-server architectures and optimized for disk IO requiring hardware scale up strategies to increase performance. In comparison, NuoDB is a distributed database designed to take advantage of modern day distributed data center or cloud architectures, optimizing for both memory and IO performance. 
    </p>
    <div style="float:left;width:32%;padding-left:5%;padding-right:10%;padding-top:1%;">
    <img src="img/architecture-workload.png" style="width: 98%;padding-left:10%;padding-right:10%;"/>
    </div>
    <p>
        Each layer can be independently scaled out to address different workload requirements. For example, the in-memory transaction processing layer can be scaled out to address read-heavy workloads (e.g. Web/Mobile Applications). Or the storage layer can be scaled out to address write heavy workloads (e.g. Logging Applications).  In addition, once the performance of a single server is exceeded, NuoDB allows users to simply add additional servers to increase database performance. This allows customers to match database resources closely to actual application demands rather than using over provisioned database systems.
    </p>
    <br>
    <h3>About The Tour</h3>
    <p>
        In this tour, you will be able to evaluate the impact of scale out and in by manually controlling the amount of client workload and number of NuoDB nodes. NuoDB can also support auto scale out by integrating with orchestration solutions, such as Amazon’s EC2 Container Service (ECS) , Kubernetes, Mesos, Nomad, Docker Swarm, etc using NuoDB’s RESTful API services.
    </p>
    <div class="row-fluid tour-figure tall">
        <div class="span2">
            <h3>When user workload increases...</h3>
            <div class="thumbnail">
                <div class="caption" style="color:black;">Workload</div>
                <img src="img/tour-up.png" height="80" />
            </div>
        </div>
        <div class="span1"></div>
        <div class="span2">
            <h3><br />...scale out by adding hosts</h3>
            <div class="thumbnail">
                <div class="caption" style="color:black;">Hosts</div>
                <img src="img/tour-up.png" height="100" />
            </div>        
        </div>
        <div class="span1"></div>
        <div class="span2">
            <h3><br />Throughput increases</h3>
            <div class="thumbnail">
                <div class="caption" style="color:black;">Throughput</div>
                <img src="img/tour-up.png" height="100" />
            </div>        
        </div>
        <div class="span2">
            <h3><br />...and latency decreases</h3>
            <div class="thumbnail">
                <div class="caption text-right" style="color:black;">Latency</div>
                <img src="img/tour-down.png" height="100" />
            </div>        
        </div>
    </div>
    <h3>Try It Yourself</h3>
    <p>
        In this tour, you will manually adjust the application workload and watch the effect on throughput and latency. As you increase the application workload you can scale out the database to meet the new demands of the application while maintaining an acceptable amount of latency.
    </p>
    <p>
        You will begin with  a minimal NuoDB deployment - one Transaction Engine and Storage Manager. To start, you will have no active workload on the database.  
    </p>
    <p>
        Steadily increase workload until you saturate the database resources and then scale out NuoDB to see the effects on latency and throughput. The workload represents an e-commerce workload with a 60/40 read/write split.
    </p>
    <h4 style="padding-top:10px;padding-bottom:10px;padding-left:50px;"> STEP 1: Add an Initial Workload to NuoDB</h4> 
        <div style="padding-left:100px;">
            <p style="width:66%;">
                Find the Workload widget in the upper left portion of the Online Experience screen.  The number in the Workload widget represents the number of simulated users interacting with the database. To start the workload, click once on the widget’s &ldquo;up&rdquo; arrow.
            </p>
            <img src="img/step1-a.png" width="200px"/>
            <p style="width:66%;">
                The workload will increase to 2,100 users.  
            </p>
            <img src="img/step1-b.png" width="200px"/>
            <p style="width:66%;">
                Within about 10s, the workload will start executing against the database. Click on the Throughput widget to view the throughput graph. 
            </p>
            <img src="img/step1-c.png" width="400px"/><br><br>
            <img src="img/step1-d.png" width="500px"/>
            <p style="width:66%;">
                Click on the Avg Latency widget to view the latency graph. There may be an initial spike in latency but this will quickly stabilize to a nominal value.
            </p>
                <img src="img/step1-e.png" width="400px"/><br><br>
                <img src="img/step1-f.png" width="500px"/>
            <p style="width:66%;">
                At 2,100 simulated users, a single NuoDB Transaction Engine can easily handle the workload while maintaining consistent low latencies.
            </p>
        </div>
    <h4 style="padding-top:10px;padding-bottom:10px;padding-left:50px;"> STEP 2: Increase Workload to Saturate NuoDB Resources</h4> 
        <div style="padding-left:100px;">  
            <p style="width:66%;">
                Click on the Workload widget’s &ldquo;up&rdquo; arrow once to increase the workload to 4,200 users. 
            </p>
            <img src="img/step2-a.png" width="200px"/>
            <p style="width:66%;">
                Navigate to the Throughput graph  to see  how the increased number of users results in a growth in throughput.  
            </p>
            <img src="img/step2-b.png" width="500px"/>
            <p style="width:66%;">
                Navigate to the Avg Latency graph.  You will see the latency graph show  significant increases in average latency. 
            </p>
            <img src="img/step2-c.png" width="500px"/>
            <p style="width:66%;">
                With a workload representing  4,200 users, the single Transaction Engine does not have sufficient system resources to maintain low latencies and fast response times. This will impact  the  user experience since  the database cannot keep up with the demands of the workload.
            </p>
        </div>
    <h4 style="padding-top:10px;padding-bottom:10px;padding-left:50px;"> STEP 3: Scale Out NuoDB to Address Increased  Application Demands</h4> 
        <div style="padding-left:100px;">
            <p style="width:66%;">
                A traditional database must be migrated to a larger server if the original database deployment cannot handle the change in application workload.  With NuoDB, you can handle  the increased application workload simply by adding another node. To accommodate our  increased user workload, scale out the database by adding another Transaction Engine. Click the &ldquo;up&rdquo; arrow in the Transaction Engines box.
            </p>
            <img src="img/step3-a.png" width="200px"/>
            <p style="width:66%;">
                Within a few seconds, the system will deploy a new Transaction Engine on a new server and balance the workload across both Transaction Engines. You’ll see an increase in throughput and latencies will drop back downs.
            </p>
            <img src="img/step3-b.png" width="500px"/><br><br>
            <img src="img/step3-c.png" width="500px"/>
        </div>
   <h4 style="padding-top:10px;padding-bottom:10px;padding-left:50px;"> STEP 4: Learn How Scaling Out NuoDB Impacts Performances</h4> 
        <div style="padding-left:100px;">
            <p style="width:66%;">
                Continue to increase the user workload and Transaction Engines independently to understand how scaling out NuoDB affects  throughput and latency. With the Scale Out Performance tour, you can apply a  maximum workload of 10,500 simulated users to the database and scale out NuoDB to 5 Transaction Engines. 
            </p>
            <p style="width:66%;">
                If you scale out to 10,500 users with 5 Transaction Engines, you should see a throughput graph that linearly increases throughput, similar to the one below:
            </p>
            <img src="img/step4-a.png" width="500px"/>
            <p style="width:66%;">
                The latency graph should show a constant nominal latency because even though the application workload has increased, the increased number of Transaction Engines can easily handle the additional workload.
            </p>
            <img src="img/step4-b.png" width="500px"/><br><br>
        </div>
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
