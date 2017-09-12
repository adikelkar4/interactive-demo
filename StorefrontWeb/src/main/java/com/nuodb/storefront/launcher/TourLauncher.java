package com.nuodb.storefront.launcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TourLauncher {
    static {
        File propertiesFile = new File(System.getProperty("catalina.base") + "/conf", "catalina.properties");
        InputStream propertiesStream;
        Properties catalinaProperties = null;

        try {
            propertiesStream = new FileInputStream(propertiesFile);
            catalinaProperties = new Properties();
            catalinaProperties.load(propertiesStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (catalinaProperties != null) {
            ecsClusterName = catalinaProperties.getProperty("ARG_ecsClusterName", "localhost");
            tourInitializationLambdaArn = catalinaProperties.getProperty("ARG_tourInitializationLambdaArn", "");
        } else {
            ecsClusterName = "localhost";
            tourInitializationLambdaArn = "";
        }
    }
	private static String ecsClusterName;
	private static String tourInitializationLambdaArn;
	
	public boolean initializeTour(Map<String, Integer> tourTopology) throws Exception {
		LambdaInput input = new LambdaInput();
		input.setTeCount(tourTopology.get("TE"));
		input.setSmCount(tourTopology.get("SM"));
		input.setMysqlCount(tourTopology.get("MYSQL"));
		input.setARG_ecsClusterName(ecsClusterName);
		AWSLambda client = AWSLambdaClientBuilder.defaultClient();
		ObjectMapper mapper = new ObjectMapper();
		InvokeRequest req = new InvokeRequest();

		req.setFunctionName(tourInitializationLambdaArn);
		req.setPayload(mapper.writeValueAsString(input));
		InvokeResult response = client.invoke(req);
		if (response.getStatusCode() - 200 < 200) { //covers redirects
			System.out.println(response.getPayload());
			return true;
		}
		return false;
	}
	
	class LambdaInput {
		private int teCount;
		private int smCount;
		private int mysqlCount;
		private String ARG_ecsClusterName;
		
		public int getTeCount() {
			return teCount;
		}
		public void setTeCount(int teCount) {
			this.teCount = teCount;
		}
		
		public int getSmCount() {
			return smCount;
		}
		public void setSmCount(int smCount) {
			this.smCount = smCount;
		}
		
		public int getMysqlCount() {
			return mysqlCount;
		}
		public void setMysqlCount(int mysqlCount) {
			this.mysqlCount = mysqlCount;
		}
		public String getARG_ecsClusterName() {
			return ARG_ecsClusterName;
		}
		public void setARG_ecsClusterName(String aRG_ecsClusterName) {
			ARG_ecsClusterName = aRG_ecsClusterName;
		}
	}
}
