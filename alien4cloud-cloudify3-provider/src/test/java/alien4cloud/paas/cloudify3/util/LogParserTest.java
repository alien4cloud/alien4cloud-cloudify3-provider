package alien4cloud.paas.cloudify3.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import alien4cloud.paas.cloudify3.shared.model.LogBatch;
import alien4cloud.paas.cloudify3.util.LogParser;

public class LogParserTest {

	@Test
	public void testStandard() {
		try {
			String response = LogParser.readFile("src/test/resources/logs/standard-logs.txt", StandardCharsets.UTF_8);
			LogBatch logBatch = LogParser.parseLog(new JSONObject(response));
			Assert.assertEquals(12, logBatch.getEntries().length);
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertNull(e);
		}
	}

	@Test
	public void testCorruptedLogs() {
		try {
			String response = LogParser.readFile("src/test/resources/logs/corrupted-logs.txt", StandardCharsets.UTF_8);
			LogBatch logBatch = LogParser.parseLog(new JSONObject(response));
			Assert.assertEquals(1, logBatch.getEntries().length);
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertNull(e);
		}
	}
}
