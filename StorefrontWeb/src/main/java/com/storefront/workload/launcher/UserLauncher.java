package com.storefront.workload.launcher;

import java.util.Map;

public interface UserLauncher {
	
	public boolean launchUser(Map<String, String> workloadOptions, int count) throws Exception;
	
}
