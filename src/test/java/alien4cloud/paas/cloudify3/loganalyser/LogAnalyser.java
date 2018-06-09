package alien4cloud.paas.cloudify3.loganalyser;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import alien4cloud.dao.IGenericSearchDAO;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import gherkin.deps.com.google.gson.Gson;
import gherkin.deps.com.google.gson.JsonArray;
import gherkin.deps.com.google.gson.JsonElement;
import gherkin.deps.com.google.gson.JsonObject;

import javax.annotation.Resource;
import javax.xml.bind.DatatypeConverter;

@Slf4j
public class LogAnalyser {

	@Test
	public void findDuplicate() throws IOException {
		Gson gson = new Gson();
		JsonObject jo = getJsonObject(gson,
				"/home/bobo/Documents/fastconnect/alien4cloud-cloudify3-provider/src/test/resources/logs/log_after_recovery.txt");
		JsonArray hits = jo.get("hits").getAsJsonObject().get("hits").getAsJsonArray();

		Set<String> set = new HashSet<>();

		for (int i = 0; i < hits.size(); i++) {
			JsonObject source = hits.get(i).getAsJsonObject().getAsJsonObject("_source");
			String content = source.get("content").getAsString();
			String timestamp = source.get("timestamp").getAsString();
			boolean added = set.add(timestamp + "-" + content);
			if (!added) {
				System.out.println(String.format("Timestamp: %s Content: %s", timestamp, content));
			}
		}
	}

	@Test
	public void compareTwoLogFile() throws FileNotFoundException {
		Gson gson = new Gson();
		String log1 = "/home/bobo/Documents/fastconnect/alien4cloud-cloudify3-provider/src/test/resources/logs/log_before_recovery.txt";
		String log2 = "/home/bobo/Documents/fastconnect/alien4cloud-cloudify3-provider/src/test/resources/logs/log_after_recovery.txt";

		List<String> list1 = createElementList(gson, log1);
		List<String> list2 = createElementList(gson, log2);

		if (list2.size() > list1.size()) {
			List<String> tmp = list1;
			list1 = list2;
			list2 = tmp;
		}

		rIterate(list1, list2, 0, 0);

	}



	private void rIterate(List<String> list1, List<String> list2, int i, int j) {
		while (i < list1.size() && j < list2.size()) {
			String content1 = list1.get(i);
			String content2 = list2.get(j);
			if (content1.equals(content2)) {
				i++;
				j++;
			} else {
				System.out.println(content2);
				j++;
			}
		}
		if (i != list1.size() && j == list2.size()) {
			System.out.println(String.format("Found content occurred in list1 but not in list2 \n%s", list1.get(i)));
			rIterate(list1, list2, i + 1, i + 1);
		}
	}

	private List<String> createElementList(Gson gson, String log) throws FileNotFoundException {
		JsonObject jo = getJsonObject(gson, log);
		JsonArray hits = jo.get("hits").getAsJsonObject().get("hits").getAsJsonArray();

		List<String> list = new LinkedList<>();

		for (int i = 0; i < hits.size(); i++) {
			JsonObject source = hits.get(i).getAsJsonObject().getAsJsonObject("_source");
			String content = source.get("content").getAsString();
			content = LogUtils.anonymize(content);
			list.add(content);
		}
		return list;
	}

	private Map<String, List<LogEntry>> createLogEntryMap(Gson gson, String log) throws FileNotFoundException {
		Map<String, List<LogEntry>> logsMap = Maps.newHashMap();

		JsonObject jo = getJsonObject(gson, log);
		JsonArray hits = jo.get("hits").getAsJsonObject().get("hits").getAsJsonArray();

		List<String> list = new LinkedList<>();

		for (int i = 0; i < hits.size(); i++) {
			JsonObject source = hits.get(i).getAsJsonObject().getAsJsonObject("_source");
			LogEntry le = createLogEntry(source);
			List<LogEntry> les = logsMap.get(le.id);
			if (les == null) {
				les = Lists.newArrayList();
				logsMap.put(le.id, les);
			}
			les.add(le);
		}

		return logsMap;
	}

	private LogEntry createLogEntry(JsonObject source) {
		String content = source.get("content").getAsString();
		String id = LogUtils.anonymize(content);
		Calendar timestamp = DatatypeConverter.parseDateTime(source.get("timestamp").getAsString());
		return new LogEntry(id, content, timestamp);
	}

	private JsonObject getJsonObject(Gson gson, String log) throws FileNotFoundException {
		JsonElement json = gson.fromJson(new FileReader(log), JsonElement.class);
		return json.getAsJsonObject();
	}

	private class LogEntry {
		private final String id;
		private final String content;
		private final Calendar timestamp;

		public LogEntry(String id, String content, Calendar timestamp) {
			this.id = id;
			this.content = content;
			this.timestamp = timestamp;
		}
	}
}
