<%-- Copyright (c) 2013-2017 NuoDB, Inc. --%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>

<t:page showHeader="false">

    <t:messages></t:messages>
    <div class="header-logo"></div>
    <h1>NUODB 101</h1>

   <h3>Introduction</h3>
        <p>NuoDB is an elastic SQL OLTP database built for hybrid cloud applications. The database scales out on-demand, ensures continuous availability, and guarantees  consistency and durability while providing standard database interfaces and operations. Applications access NuoDB via an ANSI SQL interface as a single logical database, even across data centers or Amazon Availability Zones.</br></br>
        NuoDB is well-suited for existing application migrating to the cloud (private or public) as well as applications looking to adopt modern distributed architectural designs (e.g. microservices).
        </p>
    <div style="float:left;width:30%;padding-right:10%;padding-bottom: 5%">
        <img src="img/trad-architecture.png" style="width: 90%;padding-left:10%;padding-right:10%;padding-top:10%;padding-bottom:0%;"/>
        <img src="img/architecture-workload.png" style="width: 90%;padding-left:10%;padding-right:10%;padding-top:10%;padding-bottom:0%;"/>
    </div>
    <h3>NuoDB Architecture</h3>
        <p>NuoDB features a modern architecture built for elastic scale out. In contrast to traditional single process databases, NuoDB splits traditional database processing into separate services for transaction processing and storage management. This two layer, peer-to-peer architecture is fully distributed.<br><br>
        Each layer can be independently scaled out to address different workload requirements. The in-memory transaction processing layer can be scaled out to address read-heavy workloads (e.g. Web/Mobile Applications) and/or the storage layer can be scaled out to address write heavy workloads (e.g. Logging Applications).  In addition, once the performance of the original deployment is exceeded, users can simply add additional servers to increase database performance. This allows customers to align database resources closely to actual application demands rather than over-provisioning their database systems.</p>

    <h4>Transaction Engines:</h4>
     <p>The transaction processing layer consists of in-memory processes called Transaction Engines (TEs). TEs provide high, in-memory performance access to data for applications. The in-memory cache is naturally built up based on the application’s workload. Requests for data not currently in cache (cache misses) can be fulfilled from the memory caches of other TEs or from the storage management layer.</p>
    <h5 style="color:#98a0ad;">TRANSACTION ENGINES:</h5>
     <div style="float:left;width:40px;padding: 25px;"><img src="img/TE.png"/></div>
        <ul> 
            <li>Handle requests from applications</li>
            <li>Use in-memory processing to increase performance for applications</li>
            <li>Coordinate transactions with other peer nodes in the environment</li>
            <li>Can be added or removed without any interruption to the applications</li>
        </ul>
   
    <h4>Storage Managers:</h4>
    <p>The storage management layer consists of processes called Storage Managers (SM), which have both in-memory as well as on-disk storage components. SMs provide on-disk data durability guarantees. The SM also caches data in-memory to speed up data retrieval from disk. Multiple SMs can be used to increase data redundancy or to partition  the database across servers for improved IO.</p>
    <h5 style="color:#98a0ad;">STORAGE MANAGERS:</h5>
    <div style="float:left;width:40px;padding: 25px;"><img src="img/SM.png"/></div>
        <ul>
            <li>Ensure durability of data by writing it to disk</li>
            <li>Manage data on disk</li>
            <li>Serve up data to TEs from memory or disk</li>
            <li>Maintain copies of data for redundancy</li>
            <li>Can be added or removed without any interruption to application service</li>
        </ul>
    <h3>Use Cases:</h3>
        <p>NuoDB is a general system of record database for enterprise transaction oriented operational workloads, including online transaction processing (OLTP) and hybrid transactional/analytical processing (HTAP) workloads. Customers commonly use NuoDB to replace existing legacy OLTP databases with NuoDB as they migrate to hybrid cloud environments or modernize their application architectures.. NuoDB is a popular database choice for transactional applications serving various industries such as  telecom, financial services, e-commerce, product lifecycle management, and healthcare management.</p>
    <h3>Learn More:</h3>
        <p>Read our <a href="http://go.nuodb.com/rs/139-YPK-485/images/Technical-Overview.pdf" target="_blank" style="color:#36af75;font-weight:700;">Technical Overview</a> to learn more about the overall architecture and technical benefits of NuoDB. For a deep dive into the inner workings of NuoDB, including how it processes transactions while providing ACID guarantees, check out our <a href="http://go.nuodb.com/white-paper.html" target="_blank" style="color:#36af75;font-weight:700;"> architectural white paper</a>.
        </p>

        <a href="http://go.nuodb.com/rs/139-YPK-485/images/Technical-Overview.pdf" target="_blank">
            <button class="button button1">Technical Overview</button>
        </a>
        <a href="http://go.nuodb.com/white-paper.html" target="_blank">
            <button class="button button1">Architectural White Paper</button>
        </a>

</t:page>
