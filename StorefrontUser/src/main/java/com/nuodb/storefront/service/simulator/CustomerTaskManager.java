package com.nuodb.storefront.service.simulator;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.service.ServiceRegistry;

import com.nuodb.storefront.StorefrontApp;
import com.nuodb.storefront.dal.UpperCaseNamingStrategy;
import com.nuodb.storefront.model.dto.TransactionStats;

public class CustomerTaskManager {

	SessionFactory sessionFactory;
	Configuration hibernateCfg;
	Map<String, TransactionStats> statsMap;

	public CustomerTaskManager() {
		statsMap = new HashMap<String, TransactionStats>();
		buildConfiguration();
	}

	private void buildConfiguration() {
		hibernateCfg = new Configuration();
		hibernateCfg.setNamingStrategy(new UpperCaseNamingStrategy());
		hibernateCfg.configure();

		String dbName = StorefrontApp.DB_NAME;
		String dbUser = StorefrontApp.DB_USER;
		String dbPassword = StorefrontApp.DB_PASSWORD;
		String dbOptions = StorefrontApp.DB_OPTIONS;
		if (dbName != null) {
			dbName = dbName.replace("{domain.broker}", StorefrontApp.DB_DOMAIN_BROKER);

			Matcher dbNameMatcher = Pattern.compile("([^@]*)@([^@:]*(?::\\d+|$))").matcher(dbName);
			if (!dbNameMatcher.matches()) {
				throw new IllegalArgumentException("Database name must be of the format name@host[:port]");
			}
			String name = dbNameMatcher.group(1);
			String host = dbNameMatcher.group(2);

			String url = "jdbc:com.nuodb://" + host + "/" + name;
			if (dbOptions != null) {
				url = url + "?" + dbOptions;
			}
			hibernateCfg.setProperty(Environment.URL, url);
		}
		if (dbUser != null) {
			hibernateCfg.setProperty(Environment.USER, dbUser);
		}
		if (dbPassword != null) {
			hibernateCfg.setProperty(Environment.PASS, dbPassword);
		}
	}

	protected synchronized SessionFactory getSessionFactory() {
		if (sessionFactory == null) {
			ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
					.applySettings(hibernateCfg.getProperties())
					.applySettings(Environment.getProperties())
					.build();
			sessionFactory = hibernateCfg.buildSessionFactory(serviceRegistry);
		}
		return sessionFactory;
	}

}
