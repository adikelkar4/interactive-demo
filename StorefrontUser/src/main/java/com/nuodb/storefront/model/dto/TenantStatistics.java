package com.nuodb.storefront.model.dto;

import java.util.Date;
import java.util.Map;

public class TenantStatistics {
	private String uid;
	private Date timestamp;
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
	
	public String getUid() {
		return uid;
	}
	public void setUid(String uid) {
		this.uid = uid;
	}
	
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
}
