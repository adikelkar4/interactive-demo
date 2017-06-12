var aws = require('aws-sdk');
var response = require('cfn-response');
var ecs = new aws.ECS();


exports.handler = (event, context, callback) => {
    
  aws.config.update({
    region: event.region
  });
  console.log('container image: ', event.dockerImage)
  //web params
  var web_params = {
    "containerDefinitions": [
    {
      "name": "storefront-web",
      "image": "" + event.dockerImage + "",
      "cpu": 10,
      "memory": 4096,
      "readonlyRootFilesystem": false,
      "environment": [
        {
          "name": "ENV_TYPE",
          "value": "AWSECS"
        },
        {
          "name": "JDBC_URL",
          "value": "" + event.JDBC_URL + ""
        },
        {
          "name": "BROKER_PORT",
          "value": "48004"
        }
      ],
      "essential": true,
      "portMappings": [
        {
          containerPort: 8080,
          hostPort: 8080,
          protocol: "TCP"
        }
      ],
    }
  ],
  "family": "storefront-web",
  "taskRoleArn": "" + event.role + ""
  };
  
  ecs.registerTaskDefinition(web_params, function(err, data) {
    if (err) { 
        console.log('storefront definition error: ', err, err.stack);
    } else {
        console.log('storefront definition created: ', data)
        
            //create service group for broker
    var params = {
      desiredCount: 1, 
      serviceName: "storefront-web-service",
      taskDefinition: "storefront-web",
      cluster: "" + event.cluster + "",
/*      deploymentConfiguration: {
        maximumPercent: 0,
        minimumHealthyPercent: 0
      },
*/
/*    loadBalancers: [
      {
        containerName: 'broker',
        containerPort: 48004,
        loadBalancerName: 'bhiggins-ecs-test'
      }
    ],
*/
    placementStrategy: [
      {
        field: 'instanceId',
        type: 'spread'
      }
    ],
//    role: "" + event.role + ""
  };
  ecs.createService(params, function(err, data) {
    if (err) {
        console.log('Create Service for storefront web failed: ', err, err.stack);
        response.send(event, context, response.FAILED, data);
    } else {
        console.log('Create Service for storefront successful: ', data);
        response.send(event, context, response.SUCCESS, data);
    }
  });
    }
  });
  
};