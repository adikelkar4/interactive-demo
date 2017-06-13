package com.storefront.workload.launcher;

import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;


public class LocalLauncher implements UserLauncher {

	public void launchUser(Map<String, String> dbOptions, Map<String, String> workloadOptions,
			Map<String, String> appOptions) throws Exception {
		String dbOptionsString = buildDbOptionsString(dbOptions);
		String workloadOptionsString = buildWorkloadOptionsString(workloadOptions);
		String appOptionsString = buildAppOptionsString(appOptions);
		
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

	private String buildAppOptionsString(Map<String, String> appOptions) {
		StringBuffer optionsBuffer = new StringBuffer();
		optionsBuffer.append("app.host=");
		optionsBuffer.append(appOptions.get("app.host"));
		return optionsBuffer.toString();
	}

	private String buildWorkloadOptionsString(Map<String, String> workloadOptions) {
		ArrayList<String> workloadOptionsList = new ArrayList<>();
		for (String workloadType : workloadOptions.keySet()) {			
			StringBuffer optionsBuffer = new StringBuffer();
			optionsBuffer.append("workload.");
			optionsBuffer.append(workloadType);
			optionsBuffer.append("users=");
			optionsBuffer.append(workloadOptions.get(workloadType));
			workloadOptionsList.add(optionsBuffer.toString());
		}
		return String.join(" ", workloadOptionsList);
	}

	private String buildDbOptionsString(Map<String, String> dbOptions) {
		StringBuffer optionsBuffer = new StringBuffer();
		optionsBuffer.append("db.url=");
		optionsBuffer.append(dbOptions.get("db.url"));
		optionsBuffer.append(" ");
		optionsBuffer.append("db.user=");
		optionsBuffer.append(dbOptions.get("db.user"));
		optionsBuffer.append(" ");
		optionsBuffer.append("db.password=");
		optionsBuffer.append(dbOptions.get("db.password"));
		return optionsBuffer.toString();
	}
	
}
