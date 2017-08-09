package com.nuodb.storefront.api;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.nuodb.storefront.model.dto.LogEntry;

public class AppInstanceApiTest {
	
	static List<LogEntry> testLog = new ArrayList<>();
	AppInstanceApi testApi = new AppInstanceApi();
	
	@BeforeClass
	public static void setupClass() throws Exception {
		for (int i = 0; i < 100; i++) {
			long time = System.nanoTime();
			testLog.add(new LogEntry(time, "Log Entry Number " + (i+1)));
			Thread.sleep(1);
		}
	}
	
	@Test
	public void testMiddleAsListCutoff() {
		LogEntry midEntry = testLog.get(testLog.size()/2);
		Assert.assertEquals(51, testApi.firstUnreportedEntry(testLog, midEntry.getTime(), 0, testLog.size()));
	}
	
	@Test
	public void testQuarterAsListCutoff() {
		LogEntry quarterEntry = testLog.get(testLog.size()/4);
		Assert.assertEquals(26, testApi.firstUnreportedEntry(testLog, quarterEntry.getTime(), 0, testLog.size()));
	}
	
	@Test
	public void testSeventyFifthAsListCutoff() {
		LogEntry seventyFifth = testLog.get(3 * testLog.size()/4);
		Assert.assertEquals(76, testApi.firstUnreportedEntry(testLog, seventyFifth.getTime(), 0, testLog.size()));
	}
	
	@Test
	public void testBeginningAsListCutoff() {
		LogEntry beginning = testLog.get(0);
		Assert.assertEquals(1, testApi.firstUnreportedEntry(testLog, beginning.getTime(), 0, testLog.size()));
	}
	
	@Test
	public void testEndAsListCutoff() {
		LogEntry end = testLog.get(testLog.size() - 1);
		Assert.assertEquals(-1, testApi.firstUnreportedEntry(testLog, end.getTime(), 0, testLog.size()));
	}
	
	@Test
	public void testFilterTimeBeforeFirstEntry() {
		LogEntry beginning = testLog.get(0);
		Assert.assertEquals(0, testApi.firstUnreportedEntry(testLog, beginning.getTime() - 10000, 0, testLog.size()));
	}
	
	@Test
	public void testFilterTimeBeforeLastEntry() {
		LogEntry end = testLog.get(testLog.size() - 1);
		Assert.assertEquals(testLog.size() -1, testApi.firstUnreportedEntry(testLog, end.getTime() - 10000, 0, testLog.size()));
	}
	
	@Test
	public void testFilterTimeAfterLastEntry() {
		LogEntry end = testLog.get(testLog.size() - 1);
		Assert.assertEquals(-1, testApi.firstUnreportedEntry(testLog, end.getTime() + 10000, 0, testLog.size()), testLog.size());
	}
	
	@Test
	public void testFilterTimeBetweenEntries() {
		LogEntry midEntry = testLog.get(testLog.size()/2);
		Assert.assertEquals(50, testApi.firstUnreportedEntry(testLog, midEntry.getTime() - 100000, 0, testLog.size()));
	}
	
	@Test
	public void testEmptyEntries() {
		Assert.assertEquals(-1, testApi.firstUnreportedEntry(new ArrayList<>(), testLog.get(0).getTime() - 100000, 0, testLog.size()));
	}
}
