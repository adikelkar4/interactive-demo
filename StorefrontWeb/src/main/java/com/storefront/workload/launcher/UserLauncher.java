package com.storefront.workload.launcher;

import java.util.Map;

public interface UserLauncher {
	
	public void launchUser(Map<String, String> dbOptions, Map<String, String> workloadOptions, Map<String, String> appOptions) throws Exception;
	
}
