package com.nuodb.storefront.model.dto;

/**
 * Created by Andrew on 6/7/17.
 */

import java.util.Map;

public class StatsPayload {
    private String databaseType;
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
}
