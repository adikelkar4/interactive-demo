package com.nuodb.storefront.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.codehaus.jackson.map.ObjectMapper;

import com.nuodb.storefront.model.dto.TenantStatistics;
import com.nuodb.storefront.model.dto.TransactionStats;
import com.nuodb.storefront.model.dto.WorkloadStats;

public class TenantStatisticsService implements Runnable {
	
	private TenantStatistics allStats;
	private IStorefrontTenant tenant;
	private ObjectMapper mapper;
	private String dbType;
	
	private static final String storefrontStatsEndpoint = "/api/stats";
	
	public TenantStatisticsService(IStorefrontTenant tenant, String dbVendor) {
		this.setTenant(tenant);
		allStats = new TenantStatistics();
		mapper = new ObjectMapper();
		dbType = dbVendor;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void run() {
		Map<String, TransactionStats> transactionStats = null;
		Map<String, WorkloadStats> workloadStats = null;
		synchronized (this.tenant.getTransactionStats()) {
			transactionStats = new HashMap<>(this.tenant.getTransactionStats());
			this.tenant.getTransactionStats().clear();
		}
		synchronized (this.tenant.getSimulatorService()) {
			workloadStats = new HashMap<>(this.tenant.getSimulatorService().getWorkloadStats());
			this.tenant.getSimulatorService().getWorkloadStats().clear();
		}
		Map<String, Map> payload = new HashMap<>();
		payload.put("transactionStats", transactionStats);
		payload.put("workloadStats", workloadStats);
		allStats.setDatabaseType(this.dbType);
		allStats.setPayload(payload);
		try {
			postStatsAsJson(this.tenant.getAppSettings(), allStats);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public TenantStatistics getAllStats() {
		return allStats;
	}
	public void setAllStats(TenantStatistics allStats) {
		this.allStats = allStats;
	}

	public IStorefrontTenant getTenant() {
		return tenant;
	}
	public void setTenant(IStorefrontTenant tenant) {
		this.tenant = tenant;
	}
	
	private void postStatsAsJson(Map<String, String> appSettings, TenantStatistics stats)
			throws MalformedURLException, IOException, ProtocolException {
		HttpClient client = HttpClients.createDefault();
		HttpPost postRequest = this.buildStatsConnection(appSettings);
		postRequest.setEntity(new StringEntity(mapper.writeValueAsString(stats), ContentType.APPLICATION_JSON));
		HttpResponse response = client.execute(postRequest);
		System.out.println(response.getStatusLine().getStatusCode() + ": " + response.getStatusLine().getReasonPhrase());
	}

	private HttpPost buildStatsConnection(Map<String, String> appSettings)
			throws MalformedURLException, IOException, ProtocolException {
		HttpPost postRequest = new HttpPost(appSettings.get("app.host") + storefrontStatsEndpoint);
		postRequest.addHeader("Content-Type", ContentType.APPLICATION_JSON.toString());
		return postRequest;
	}
}
