<%-- Copyright (c) 2013-2017 NuoDB, Inc. --%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>

<t:page showHeader="false">
    <div class="header-logo"></div>
    <h1>NUODB 101</h1>
    
   <h3>Introduction</h3>
        <p>NuoDB is an elastic SQL database for hybrid cloud applications.  The database scales-out on-demand, ensures continuous availability, and provides standard database interfaces, operations, and guarantees of consistency and durability. Applications access NuoDB via an ANSI SQL interface and as a single logical database, even across data centers or Amazon availability zones.</br></br>
        Particularly well-suited for hybrid cloud, distributed and microservices architectures, container environments, and dynamic operational workloads, NuoDB also preserves the best of the traditional database world via support of an ANSI SQL interface and full ACID compliance.
        </p>
    <h3>Use Cases</h3>
        <p>NuoDB is designed to be a general system of record database for enterprise operational workloads, including online transaction processing (OLTP) and hybrid transactional/analytical (HTAP) processing workloads. Well-suited as a technology for software development organizations that service a diverse range of industries, NuoDB is built for the modern data center, hybrid cloud environments, and for adaptability to efficiently handle a broad range of transactional workloads. Popular use cases for NuoDB include software services within telecom, financial services, e-commerce, product lifecycle management, and healthcare management industries.</p>
        <div style="float:left;width:40%;padding-right:10%;"><img src="img/network-architecture.png" style="width: 90%;padding-left:10%;padding-right:10%;padding-top:20%;padding-bottom:20%;"/></div>
<h3>NuoDB Architecture: The Durable Distributed Cache</h3>        

        <p>NuoDB features the Durable Distributed Cache, a modern architecture built for elastic scale out. In contrast to traditional monolithic databases, NuoDB uses separate services for transaction processing and storage management. This two layer, peer-to-peer architecture enables distributed processing that can be deployed across multiple data centers and is optimized for in-memory speeds, continuous availability, and elastic scale-out.
        </p>
        <p>The transaction processing layer consists of in-memory process nodes called Transaction Engines (TE). TEs allow NuoDB to maintain high, in-memory performance. As an application makes requests of NuoDB, TEs naturally build up in-memory caches with afnity for that application’s workload. Requests for data not currently in cache (cache misses) can be fulflled from the memory caches of other TEs or from the storage management layer.</br>
    <h4>Transaction Engines:</h4>
    <div style="float:left;width:40px;padding: 25px;"><img src="img/TE.png"/></div>
        <ul> 
            <li>Handle requests from applications</li>
            <li>Use in-memory processing to increase performance for applications</li>
            <li>Coordinate transactions with other peer nodes in the environment</li>
            <li>Can be added or removed without any interruption to the applications</li>
        </ul>
    <p>The storage management layer consists of process nodes called Storage Managers (SM), which have both in-memory as well as on-disk storage components. The SM also caches data in-memory to speed up data retrieval from disk. SMs also provide on-disk data durability guarantees. Multiple SMs can be used to increase data redundancy.</p>
    <h4>Storage Managers:</h4>
    <div style="float:left;width:40px;padding: 25px;"><img src="img/SM.png"/></div>
        <ul>
            <li>Ensure durability of data by writing it to disk</li>
            <li>Manage data on disk</li>
            <li>Serve up data to TEs from memory or disk</li>
            <li>Maintain copies of data for redundancy</li>
            <li>Can be added or removed without any interruption to application service</li>
        </ul>

<p>Without any need for application data management, and without any interruption to application service, NuoDB’s transaction processing and storage management services can elastically scale out (and back) simply by adding and removing TEs and SMs. </p>
    
    <h3>Learn More:</h3>
        <p>Read our <a href="http://go.nuodb.com/rs/139-YPK-485/images/Technical-Overview.pdf" target="_blank" style="color:#36af75;font-weight:700;">Technical Overview</a> to learn more about the overall architecture and technical benefits of NuoDB.  For a deep dive into the inner workings of NuoDB, including how it processes transactions while providing ACID guarantees, check out our <a href="http://go.nuodb.com/white-paper.html" target="_blank" style="color:#36af75;font-weight:700;"> architectural white paper</a>.
        </p>

</t:page>
