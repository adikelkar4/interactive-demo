package com.storefront.workload.launcher;

import java.util.Map;

public interface UserLauncher {
	
	public void launchUser(Map<String, String> workloadOptions, int count) throws Exception;
	
}
