package com.storefront.workload.launcher;

import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;


public class LocalLauncher implements UserLauncher {
	
	private Map<String, String> appOptions;
	private Map<String, String> dbOptions;

	public void launchUser(Map<String, String> workloadOptions, int count) throws Exception {
		String workloadOptionsString = buildWorkloadOptionsString(workloadOptions);
		String dbOptionsString = buildDbOptionsString();
		String appOptionsString = buildAppOptionsString();
		Process process = Runtime.getRuntime().exec("java -jar /Users/kwhite/interactive-demo/StorefrontUser/target/StorefrontUser.jar " + dbOptionsString + " " + workloadOptionsString + " " + appOptionsString);
		Scanner outputScanner = new Scanner(process.getInputStream());
		Scanner errorScanner = new Scanner(process.getErrorStream());
		process.waitFor();
		if (process.exitValue() != 0) {
			while(outputScanner.hasNextLine()) {
				System.out.println(errorScanner.nextLine());
			}
		}
		while (outputScanner.hasNextLine()) {
			System.out.println(outputScanner.nextLine());
		}
	}

	private String buildWorkloadOptionsString(Map<String, String> workloadOptions) {
		ArrayList<String> workloadOptionsList = new ArrayList<>();
		for (String workloadType : workloadOptions.keySet()) {			
			StringBuffer optionsBuffer = new StringBuffer();
			optionsBuffer.append("workload.");
			optionsBuffer.append(workloadType);
			optionsBuffer.append(".users=");
			optionsBuffer.append(workloadOptions.get(workloadType));
			workloadOptionsList.add(optionsBuffer.toString());
		}
		return String.join(" ", workloadOptionsList);
	}
	
	private String buildDbOptionsString() {
		 StringBuffer optionsBuffer = new StringBuffer();
		 optionsBuffer.append("db.url=");
		 optionsBuffer.append(getDbOptions().get("db.url"));
		 optionsBuffer.append(" ");
		 optionsBuffer.append("db.user=");
		 optionsBuffer.append(getDbOptions().get("db.user"));
		 optionsBuffer.append(" ");
		 optionsBuffer.append("db.password=");
		 optionsBuffer.append(getDbOptions().get("db.password"));
		 return optionsBuffer.toString();
	}
	
	private String buildAppOptionsString() {
		 StringBuffer optionsBuffer = new StringBuffer();
		 optionsBuffer.append("app.host=");
		 optionsBuffer.append(getAppOptions().get("app.host"));
		 return optionsBuffer.toString();
	}

	public Map<String, String> getDbOptions() {
		return dbOptions;
	}
	public void setDbOptions(Map<String, String> dbOptions) {
		this.dbOptions = dbOptions;
	}

	public Map<String, String> getAppOptions() {
		return appOptions;
	}
	public void setAppOptions(Map<String, String> appOptions) {
		this.appOptions = appOptions;
	}
}
