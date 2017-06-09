package com.nuodb.storefront.service;

import java.util.Map;

public class TenantStatistics {
	private String databaseType;
	@SuppressWarnings("rawtypes")
	private Map<String, Map> payload;
	
	public String getDatabaseType() {
		return databaseType;
	}
	public void setDatabaseType(String newValue) {
		this.databaseType = newValue;
	}
	
	@SuppressWarnings("rawtypes")
	public Map<String, Map> getPayload() {
		return payload;
	}
	public void setPayload(@SuppressWarnings("rawtypes") Map<String, Map> newValue) {
		this.payload = newValue;
	}
}
