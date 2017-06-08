package com.nuodb.storefront.service;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;

import com.nuodb.storefront.model.dto.TransactionStats;
import com.nuodb.storefront.model.dto.WorkloadStats;

public class TenantStatisticsService implements Runnable {
	
	private TenantStatistics allStats;
	private IStorefrontTenant tenant;
	private ObjectMapper mapper;
	
	public TenantStatisticsService(IStorefrontTenant tenant) {
		this.setTenant(tenant);
		allStats = new TenantStatistics();
		mapper = new ObjectMapper();
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
		payload.put("transactionStatistics", transactionStats);
		payload.put("workloadStatistics", workloadStats);
		allStats.setDatabaseType("nuodb");
		allStats.setPayload(payload);
//		try {
//			postStatsAsJson(this.tenant.getAppSettings(), allStats);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
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
		HttpURLConnection connection = buildStatsConnection(appSettings, "POST");
		DataOutputStream stream = new DataOutputStream(connection.getOutputStream());
		stream.writeBytes(mapper.writeValueAsString(stats));
		stream.flush();
		stream.close();
	}

	private static HttpURLConnection buildStatsConnection(Map<String, String> appSettings, String method)
			throws MalformedURLException, IOException, ProtocolException {
		URL url = new URL(appSettings.get("app.url"));
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod(method);
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setDoOutput(true);
		return connection;
	}
}
