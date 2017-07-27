package com.storefront.workload.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Properties;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AwsHostLauncher implements HostLauncher {
    static {
        File propertiesFile = new File(System.getProperty("catalina.base") + "/conf", "catalina.properties");
        InputStream propertiesStream;
        Properties catalinaProperties = null;

        try {
            propertiesStream = new FileInputStream(propertiesFile);
            catalinaProperties = new Properties();
            catalinaProperties.load(propertiesStream);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (catalinaProperties != null) {
            ecsClusterName = catalinaProperties.getProperty("ARG_ecsClusterName", "localhost");
            hostLambdaArn = catalinaProperties.getProperty("ARG_teScalingLambdaArn", "");
        } else {
            ecsClusterName = "localhost";
            hostLambdaArn = "";
        }
    }

    private static String ecsClusterName;
    private static String hostLambdaArn;

    @Override
    public void scaleHosts(int count) throws Exception {
        LambdaInput input = new LambdaInput();
        input.setCount(count);

        AWSLambda client = AWSLambdaClientBuilder.defaultClient();
        ObjectMapper mapper = new ObjectMapper();
        InvokeRequest req = new InvokeRequest();

        req.setFunctionName(hostLambdaArn);
        req.setPayload(mapper.writeValueAsString(input));
        System.out.println(client.invoke(req).getPayload());

        return;
    }

    class LambdaInput {
        private int count;

        public int getCount() {
            return this.count;
        }

        public void setCount(int count) {
            this.count = count;

            return;
        }
    }
}
