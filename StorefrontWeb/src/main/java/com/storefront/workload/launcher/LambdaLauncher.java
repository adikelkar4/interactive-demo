package com.storefront.workload.launcher;

import java.util.Map;

import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.invoke.LambdaFunction;
import com.amazonaws.services.lambda.invoke.LambdaInvokerFactory;

public class LambdaLauncher implements UserLauncher {

	@Override
	public void launchUser(Map<String, String> workloadOptions, int count) throws Exception {
		LambdaInput input = new LambdaInput();
		input.setCount(count);
		for (String option : workloadOptions.keySet()) {
			switch(option.toLowerCase()) {
			case "multi_browse":
				input.setMulti_browse(workloadOptions.get(option));
				break;
			case "multi_browse_and_review":
				input.setMulti_browse_and_review(workloadOptions.get(option));
				break;
			case "shopper":
				input.setShopper(workloadOptions.get(option));
				break;
			case "analyst":
				input.setAnalyst(workloadOptions.get(option));
				break;
			}
		}
		LambdaService service = LambdaInvokerFactory.builder()
		 .lambdaClient(AWSLambdaClientBuilder.defaultClient())
		 .build(LambdaService.class);
		LambdaOutput output = service.launchContainer(input);
		System.out.println(output);
	}
	
	class LambdaInput {
		private int count;
		private String multi_browse;
		private String multi_browse_and_review;
		private String shopper;
		private String analyst;
		
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
	
	interface LambdaService {
		@LambdaFunction(functionName="test-cluster-elb-buld-6-deployUserContainer-3DF4J0H6S14M")
		LambdaOutput launchContainer(LambdaInput input);
	}
}
