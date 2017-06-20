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

public class LambdaLauncher implements UserLauncher {
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
			userLoadLambdaArn = catalinaProperties.getProperty("ARG_userLoadLambdaArn", "");
		} else {
			ecsClusterName = "localhost";
			userLoadLambdaArn = "";
		}
	}
	
	private static String ecsClusterName;
	private static String userLoadLambdaArn;
	
	@Override
	public void launchUser(Map<String, String> workloadOptions, int count) throws Exception {
		LambdaInput input = new LambdaInput();
		input.setCount(count);
		input.setARG_ecsClusterName(getEcsClusterName());

		for (String option : workloadOptions.keySet()) {
			switch(option.toLowerCase()) {
			case "multi_browse":
				input.setMulti_browse(workloadOptions.get(option));
				break;
			case "multi_browse_and_review":
				input.setMulti_browse_and_review(workloadOptions.get(option));
				break;
			case "multi_shop":
				input.setShopper(workloadOptions.get(option));
				break;
			case "admin_run_report":
				input.setAnalyst(workloadOptions.get(option));
				break;
			}
		}

		AWSLambda client = AWSLambdaClientBuilder.defaultClient();
		ObjectMapper mapper = new ObjectMapper();
		InvokeRequest req = new InvokeRequest();

		req.setFunctionName(userLoadLambdaArn);
		req.setPayload(mapper.writeValueAsString(input));
		System.out.println(client.invoke(req).getPayload());

		return;
	}
	
	public String getEcsClusterName() {
		return ecsClusterName;
	}

	public void setEcsClusterName(String newValue) {
		ecsClusterName = newValue;
	}

	class LambdaInput {
		private int count;
		private String multi_browse;
		private String multi_browse_and_review;
		private String shopper;
		private String analyst;
		private String ARG_ecsClusterName;
		
		public int getCount() {
			return count;
		}
		public void setCount(int count) {
			this.count = count;
		}
		
		public String getMulti_browse() {
			return multi_browse;
		}
		public void setMulti_browse(String multi_browse) {
			this.multi_browse = multi_browse;
		}
		
		public String getMulti_browse_and_review() {
			return multi_browse_and_review;
		}
		public void setMulti_browse_and_review(String multi_browse_and_review) {
			this.multi_browse_and_review = multi_browse_and_review;
		}
		
		public String getShopper() {
			return shopper;
		}
		public void setShopper(String shopper) {
			this.shopper = shopper;
		}
		
		public String getAnalyst() {
			return analyst;
		}
		public void setAnalyst(String analyst) {
			this.analyst = analyst;
		}
		
		public String getARG_ecsClusterName() {
			return ARG_ecsClusterName;
		}
		public void setARG_ecsClusterName(String aRG_ecsClusterName) {
			ARG_ecsClusterName = aRG_ecsClusterName;
		}
	}
	
	class LambdaOutput {
		private String Data;

		public String getData() {
			return Data;
		}

		public void setData(String data) {
			Data = data;
		}
	}
}
