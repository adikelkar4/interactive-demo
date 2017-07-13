package com.nuodb.storefront.model.dto;

import java.util.Date;

/**
 * Created by Andrew on 6/7/17.
 */

import java.util.Map;

public class StatsPayload {
    private String databaseType;
    private String uid;
    private Date timestamp;
    private Map<String, Map> payload;

    public String getDatabaseType() {
        return this.databaseType;
    }

    public void setDatabaseType(String type) {
        this.databaseType = type;

        return;
    }

    public Map<String, Map> getPayload() {
        return this.payload;
    }

    public void setPayload(Map payload) {
        this.payload = payload;

        return;
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
