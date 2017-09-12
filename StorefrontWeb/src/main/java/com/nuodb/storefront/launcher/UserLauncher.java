package com.nuodb.storefront.launcher;

import java.util.Map;

public interface UserLauncher {
	
	public boolean launchUser(Map<String, String> workloadOptions, int count) throws Exception;
	
}
