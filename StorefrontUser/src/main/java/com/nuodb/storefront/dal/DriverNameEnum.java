package com.nuodb.storefront.dal;

public enum DriverNameEnum {
	NUODB("com.nuodb"),
	MYSQL("mysql"),
	POSTGRES("pg"),
	AURORA("com.amazon.aurora");
	
	private String driverName;
	
	private DriverNameEnum(String packageName) {
		this.driverName = packageName;
	}
	
	public String toString() {
		return driverName;
	}
}
