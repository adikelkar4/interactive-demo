NuoDB Storefront Demo
=====================

This mock storefront web application showcases NuoDB's 5 value propositions:  scale-out performance, continuous availability, region distribution, multi-tenant, and no-knobs administration.  You can browse products, add items to you cart, write reviews, and checkout.  You can also simulate thousands of concurrent (simulated) shoppers with customizable workload characteristics. While the store itself is not really open for business, the queries being run under the hood are quite real!  

![ScreenShot](doc/home.png)


Quickstart
----------

To run the Storefront Demo in a demo cluster on Amazon, assuming you
have your `~/.aws/credentials` configured correctly, run...

```
bin/cluster create && bin/cluster list
```

...and point a web browser at the URL given in the output

See [the detailed documentation](#the-demo-cluster-manipulation-tool)
for further information on this tool.


Prerequisites
-------------

1. NuoDB 2.1 or higher.  Earlier versions of Storefront supported MySQL and other RDBMSs, which you can find in the [rel/2.0.6 branch](https://github.com/nuodb/nuodb-samples/tree/rel/2.0.6/StorefrontDemo) (or earlier).

2. NuoDB Rest Service.  This exposes the NuoDB API used by the Storefront its Control Panel features.

3. [Apache Maven 3](http://maven.apache.org/download.cgi) or higher.  This tool is used to build the Storefront
   from source and run it using a Tomcat web server.  Maven fetches all the other dependencies you need automatically.

4. [Python 2.7](https://www.python.org/downloads/).  This allows us to run scripts for cloud deployment.

5. [PIP](https://pip.pypa.io/en/stable/installing/).  This will bring all dependencies for our Python scripts together
   automatically for you.


Getting Started (AWS)
---------------

1. [AWS CLI](https://aws.amazon.com/cli/).  Install this in order to execute code against an AWS account.  Must be
   configured with an existing AWS account using `aws configure`.

2. Grab the source code from [Git](https://github.com/nuodb/interactive-demo).

3. From the root directory (interactive-demo), run the following PIP command to download all dependencies:

        pip install -r requirements.txt

4. Run the following command to create a new cluster:

        bin/cluster create


Getting Started (command line)
---------------

1. Grab the source code from [Git](https://github.com/nuodb/interactive-demo).

2. Download and install [Apache Maven 3](http://maven.apache.org/download.cgi) or higher.  This tool is used to build the Storefront
   from source and run it using a Tomcat web server.  Maven fetches all the other dependencies you need automatically.

3. Run the Storefront web app:

        cd interactive-demo\StorefrontWeb
        mvn tomcat7:run [args]

   The following (optional) Storefront environment settings may be provided:

        -Dstorefront.url={protocol}://{host}:{port}/{context} 

      >	The externally-accessible URL of the Storefront.  Web browsers should be able to access the Storefront
      >	web app using this URL.  The URL is used by the Storefront front-end for communication and switching among instances.
      > You may use leave any or all of the `{protocol}`, `{host}`, `{port}`, and `{context}` placeholders verbatim for auto-detected values, 
      > or specify a completely custom URL with no placeholders.  Example: `http://192.168.1.50/{context}`
      >    
      > If you do not specify a command line value, the default is pulled from the `public-url` context param of web.xml.
      > The default is `{protocol}://{host}:{port}/{context}`.  You may use the `{domain.broker}` placeholder, which will be replaced
      > with the system property `domain.broker` (defaulting to `localhost`). 

		-Dstorefront.db.name=dbname@host[:port]

	  > The database name (dbname) and NuoDB broker hostname or IP address (host).  The Storefront creates its database and schema automatically at startup,
	  > so you need not create the database in advance. 

		-Dstorefront.db.user=StorefrontUser

	  > The username of the database account to use when connecting.  If the database does not yet exist, Storefront will add this user when creating the database.

		-Dstorefront.db.password=StorefrontUser

	  > The password of the database account to use when connecting. 

		-Dstorefront.db.options=

	  > Querystring parameters to include as part of the JDBC connection string.  

		-Dstorefront.dbapi.user=domain
		-Dstorefront.dbapi.password=bird
		-Dstorefront.dbapi.host=localhost
		-Dstorefront.dbapi.port=8888

	  > Credentials and endpoint information for connecting to NuoDB's AutoConsole API (with defaults shown above).  The API is used for Control Panel tasks, such
	  > as creating the database, adding/removing hosts and regions, shutting down nodes on demand, etc.  

   You may bundle these Storefront settings in a properties file containing the key=value pairs to use instead of, or as overrides to, 
   the above command line arguments.

		-Dproperties={filename}

   The Maven Tomcat plugin also supports [some settings](http://tomcat.apache.org/maven-plugin-2.1/tomcat7-maven-plugin/run-mojo.html), including:

		-Dmaven.tomcat.port=8080


4. Explore the web app at `http://localhost:8080/StorefrontDemo` (or whichever port you've chosen).


Getting Started (Eclipse)
---------------

See the [Storefront Demo Developer Setup Guide](doc/NuoDB-Storefront.ppt) for step-by-step instructions with screenshots.


StorefrontApp Command Line Utility
-----------------------------------

`com.nuodb.storefront.StorefrontApp` supports the following actions via command line arguments.  

- `create` -- create schema
- `drop` -- drop schema
- `showddl` -- display drop and create DDL
- `generate` -- generate dummy storefront data
- `load` -- load storefront data from src/main/resources/sample-products.json file
- `simulate` -- simulate customer activity with a mix of workloads for 100 seconds
- `benchmark` -- run benchmark simulation for 1 minute


If you specify multiple actions, they are executed in sequence.  For example, to recreate the schema,  initialize it with about 1,000 products, and then stress test the app with simulated load for 1 minute, specify the command line "drop create load simulate".


Storefront Components
-------
The store itself has 4 pages:

1. Product listing page (as shown above)
2. Product details page
3. Product review form
4. Cart contents and checkout page

There are guided tour pages that demonstrate NuoDB's 5 value propositions using Storefront functionality:

1. Scale-out performance
2. Continuous availability
3. Region distribution
4. Multi-tenant
5. No-knobs administration

Finally, there are several "control panel" pages for detailed information on NuoDB and fine-grained control over behavior:

1. Products
2. Simulated users
3. Database
4. Hosts & processes
5. Regions


Key Libraries Used
----------------------------------
Server side librares:
- **Jersey** -- JSON-based RESTful API
- **Hibernate** -- ORM mapping
- **NuoDB JDBC driver, Hibernate dialect, and DataSource connection pool**
- **GoogleCode Generic DAO** -- thin data access wrapper on Hibernate for searching, saving, etc.

Client-side libraries:
- **Twitter Bootstrap** -- look & feel
- **Handlebars** -- HTML templating
- **jQuery**
- **jQuery RateIt plug-in** -- star ratings
- **less CSS** -- Dynamic stylesheets

Admin client-side libraries:
- **Sencha Ext JS** -- look & feel
- **jQuery Sparkline plug-in** -- sparklines in the header



The Demo Cluster Manipulation Tool
==================================

```
usage: cluster [-h] [--profile PROFILE] [--parallel PARALLEL]
               {create,list,delete} ...

Manage Demo Clusters

positional arguments:
  {create,list,delete}  sub-command help
    create              Create one or more clusters
    list                List clusters
    delete              Delete clusters

optional arguments:
  -h, --help            show this help message and exit
  --profile PROFILE     The AWS profile to use
  --parallel PARALLEL   Number of operations to perform in parallel
```


Creating Demo Clusters
----------------------

```
bin/cluster create
```

Will use the AWS 'default' profile to create a NuoDB demo cluster and,
when created, report it's URL.

You may also create multiple clusters at one time with e.g.

```
bin/cluster create --count 10
```

By default, the cluster name will contain the current user's login
name and a timestamp.  You may use the `--user` and `--suffix` to
change the name of the clusters.


Full Usage:

```
$ bin/cluster create -h
usage: cluster create [-h] [--user USER] [--prefix PREFIX] [--no-wait]
                      [--dry-run] [--params-file PARAMS_FILE]
                      [--template TEMPLATE]
                      [--count COUNT | --suffix SUFFIX | --key-name KEY_NAME]

optional arguments:
  -h, --help            show this help message and exit
  --user USER           user name to include in cluster name
  --prefix PREFIX       cluster name prefix
  --no-wait             Do not wait for cluster completion
  --dry-run             Do not actually create the stack
  --params-file PARAMS_FILE
                        Parameters file
  --template TEMPLATE   CFN Template file
  --count COUNT         number of clusters to create
  --suffix SUFFIX       cluster name suffix. Default is a timestamp
  --key-name KEY_NAME   SSH key name for instance
```

Listing Demo Clusters
---------------------

```
bin/cluster list
```

Will list all clusters with the current user's username in the title,
along with the status of the cloudformation stack that creates them.

Full Usage:
```
$ bin/cluster list -h
usage: cluster list [-h] [--include INCLUDE] [--exclude EXCLUDE]

optional arguments:
  -h, --help         show this help message and exit
  --include INCLUDE  include clusters containing
  --exclude EXCLUDE  exclude clusters containing
``` 


Deleting a Demo Cluster
-----------------------

```
bin/cluster delete 
```

This will delete all clusters with the current user in the cluster
name.

Other optionas `--include` and `--exclude` allow explicit
specification of clusters to delete by substring of the cluster name.

If multiple clusters are selected, they are deleted in parallel.

Full Usage:

```
$ bin/cluster delete -h
usage: cluster delete [-h] [--include INCLUDE] [--exclude EXCLUDE]

optional arguments:
  -h, --help         show this help message and exit
  --include INCLUDE  include clusters containing
  --exclude EXCLUDE  exclude clusters containing
```

Example of cluster listing and deletion
---------------------------------------

```
$ bin/cluster --profile nuodb_profile list
interactive-demo-dewey-20170629-152634 CREATE_COMPLETE http://interacti-Storefro-1C7H76P7HVOZB-1318389021.us-east-2.elb.amazonaws.com/StorefrontWeb/
interactive-demo-dewey-20170629-152626 CREATE_COMPLETE http://interacti-Storefro-4B7NMPD6W5RV-1221034874.us-east-2.elb.amazonaws.com/StorefrontWeb/
interactive-demo-dewey-20170629-152609 CREATE_COMPLETE http://interacti-Storefro-7YBVA3P9NB9L-1791407450.us-east-2.elb.amazonaws.com/StorefrontWeb/
interactive-demo-dewey-20170629-152600 CREATE_COMPLETE http://interacti-Storefro-1NU6MNNW84HJX-743284347.us-east-2.elb.amazonaws.com/StorefrontWeb/
interactive-demo-dewey-20170629-151448 DELETE_FAILED
interactive-demo-dewey-20170629-151440 DELETE_FAILED
interactive-demo-dewey-20170629-151431 DELETE_FAILED

$ bin/cluster --profile nuodb_profile delete --include interactive-demo-dewey-20170629-152600
Deleting cluster interactive-demo-dewey-20170629-152600
...interactive-demo-dewey-20170629-152600: deleting stack
...interactive-demo-dewey-20170629-152600: deletion blocked by undeleted resources
...interactive-demo-dewey-20170629-152600: deleting ECS cluster
...interactive-demo-dewey-20170629-152600: downscaling services
...interactive-demo-dewey-20170629-152600: deleting services
...interactive-demo-dewey-20170629-152600: deleting ecs cluster
...interactive-demo-dewey-20170629-152600: deleting stack
SUCCESS deleting interactive-demo-dewey-20170629-152600

$ bin/cluster --profile nuodb_profile delete --include 151431
Deleting cluster interactive-demo-dewey-20170629-151431
...interactive-demo-dewey-20170629-151431: deleting stack
...interactive-demo-dewey-20170629-151431: SUCCESS deleting stack
SUCCESS deleting cluster interactive-demo-dewey-20170629-151431
```

Deletion of multiple stacks is done in parallel:

```
$ bin/cluster --profile nuodb_profile list
interactive-demo-dewey-20170629-152634 CREATE_COMPLETE http://interacti-Storefro-1C7H76P7HVOZB-1318389021.us-east-2.elb.amazonaws.com/StorefrontWeb/
interactive-demo-dewey-20170629-152626 CREATE_COMPLETE http://interacti-Storefro-4B7NMPD6W5RV-1221034874.us-east-2.elb.amazonaws.com/StorefrontWeb/
interactive-demo-dewey-20170629-152609 CREATE_COMPLETE http://interacti-Storefro-7YBVA3P9NB9L-1791407450.us-east-2.elb.amazonaws.com/StorefrontWeb/
interactive-demo-dewey-20170629-151448 DELETE_FAILED
interactive-demo-dewey-20170629-151440 DELETE_FAILED

$ bin/cluster --profile nuodb_profile list --exclude 152634
interactive-demo-dewey-20170629-152626 CREATE_COMPLETE http://interacti-Storefro-4B7NMPD6W5RV-1221034874.us-east-2.elb.amazonaws.com/StorefrontWeb/
interactive-demo-dewey-20170629-152609 CREATE_COMPLETE http://interacti-Storefro-7YBVA3P9NB9L-1791407450.us-east-2.elb.amazonaws.com/StorefrontWeb/
interactive-demo-dewey-20170629-151448 DELETE_FAILED
interactive-demo-dewey-20170629-151440 DELETE_FAILED

$ bin/cluster --profile nuodb_profile delete --exclude 152634
Deleting cluster interactive-demo-dewey-20170629-152626
...interactive-demo-dewey-20170629-152626: deleting stack
Deleting cluster interactive-demo-dewey-20170629-152609
...interactive-demo-dewey-20170629-152609: deleting stack
Deleting cluster interactive-demo-dewey-20170629-151448
...interactive-demo-dewey-20170629-151448: deleting stack
Deleting cluster interactive-demo-dewey-20170629-151440
...interactive-demo-dewey-20170629-151440: deleting stack
...interactive-demo-dewey-20170629-151440: SUCCESS deleting stack
SUCCESS deleting cluster interactive-demo-dewey-20170629-151440
...interactive-demo-dewey-20170629-151448: SUCCESS deleting stack
SUCCESS deleting cluster interactive-demo-dewey-20170629-151448
...interactive-demo-dewey-20170629-152609: deletion blocked by undeleted resources
...interactive-demo-dewey-20170629-152609: deleting ECS cluster
...interactive-demo-dewey-20170629-152609: downscaling services
...interactive-demo-dewey-20170629-152609: deleting services
...interactive-demo-dewey-20170629-152609: deleting ecs cluster
...interactive-demo-dewey-20170629-152609: deleting stack
SUCCESS deleting interactive-demo-dewey-20170629-152609
...interactive-demo-dewey-20170629-152626: deletion blocked by undeleted resources
...interactive-demo-dewey-20170629-152626: deleting ECS cluster
...interactive-demo-dewey-20170629-152626: downscaling services
...interactive-demo-dewey-20170629-152626: deleting services
...interactive-demo-dewey-20170629-152626: deleting ecs cluster
...interactive-demo-dewey-20170629-152626: deleting stack
SUCCESS deleting interactive-demo-dewey-20170629-152626

$ bin/cluster --profile nuodb_profile  list
interactive-demo-dewey-20170629-152634 CREATE_COMPLETE http://interacti-Storefro-1C7H76P7HVOZB-1318389021.us-east-2.elb.amazonaws.com/StorefrontWeb/
```