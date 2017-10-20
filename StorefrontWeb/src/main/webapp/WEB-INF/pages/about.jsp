<%-- Copyright (c) 2013-2017 NuoDB, Inc. --%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@taglib prefix="t" tagdir="/WEB-INF/tags"%>

<t:page showHeader="false">
    <t:messages></t:messages>
    <div class="header-logo"></div>
    <h1>ABOUT THIS DEMO</h1>
    
    <h3>Introduction</h3>
        <p>The NuoDB Online Experience packages NuoDB into Docker containers and deploys them on AWS.</p>
    <h3>How It's Built</h3>
        <p>
            The NuoDB Online Experience uses AWS EC2 and ECS services. ECS is used to host the Workload containers and EC2 is used to host the NuoDB containers. As part of the deployment script, seven EC2 m4.xlarge instances  are pre-provisioned when the environment is created.
        </p>
        <div style="float:right;width:33%;padding-right:10%;padding-left:5%;padding-bottom: 5%">
            <img src="img/aws-diagram.png" style="padding-left:10%;padding-top:3%;"/>
        </div>
        <h5>
            The pre-provisioned EC2 instances include:
        </h5>
        <ul>
            <li>a NuoDB management Broker process, which performs administration of NuoDB and load balances the workload to available Transaction Engines</li>
            <li>a NuoDB Storage Manager, used to provide  data durability</li>
            <li>a NuoDB Transaction Engine, which is used to process transactions</li>
            <li>Four EC2 instances available to host the NuoDB Transaction Engines when they are dynamically added to the environment</li>
        </ul>
        <p>
            By pre-provisioning ECS instances, Transaction Engines can be quickly started using  a simple docker start command.
        </p>
        <p>
            For production environments, these deployment scripts can be easily changed to provision the EC2 instances on an as-needed basis.This approach will increase the startup time for the Transaction Engine, but will avoid over-provisioning AWS resources before they are needed for use.
        </p>

    <h3>Build Your Own</h3>
        <p>Experience the demo in your own environment! Dive into the details of the application and NuoDB by downloading the code and following instructions in the Github repository.</p>
    
        <a href="https://github.com/nuodb/interactive-demo" target="_blank">
            <button class="button button1">Download the Code</button>
        </a>
        <a href="https://github.com/nuodb/interactive-demo/blob/master/README.md" target="_blank">
            <button class="button button1">Demo Implementation Documentation</button>
        </a>

</t:page>
