package com.storefront.workload.launcher;

import java.util.ArrayList;
import java.util.Map;
import java.util.Scanner;


public class LocalLauncher implements UserLauncher {

	public void launchUser(Map<String, String> workloadOptions, int count) throws Exception {
		String workloadOptionsString = buildWorkloadOptionsString(workloadOptions);
		Process process = Runtime.getRuntime().exec("java -jar /Users/kwhite/interactive-demo/StorefrontUser/target/StorefrontUser.jar " + workloadOptionsString);
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
			optionsBuffer.append("users=");
			optionsBuffer.append(workloadOptions.get(workloadType));
			workloadOptionsList.add(optionsBuffer.toString());
		}
		return String.join(" ", workloadOptionsList);
	}
}
