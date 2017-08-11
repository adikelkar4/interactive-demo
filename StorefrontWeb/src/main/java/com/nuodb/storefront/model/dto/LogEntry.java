package com.nuodb.storefront.model.dto;

public class LogEntry {
	private Long time;
	private String message;
	
	public LogEntry() {}
	
	public LogEntry(Long time, String text) {
		this.time = time;
		this.message = text;
	}
	
	public Long getTime() {
		return time;
	}
	public void setTime(Long time) {
		this.time = time;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
}
