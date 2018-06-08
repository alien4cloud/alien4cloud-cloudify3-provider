package alien4cloud.paas.cloudify3;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import gherkin.deps.com.google.gson.Gson;
import gherkin.deps.com.google.gson.JsonArray;
import gherkin.deps.com.google.gson.JsonElement;
import gherkin.deps.com.google.gson.JsonObject;

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
			content = anonymize(content);
			list.add(content);
		}
		return list;
	}

	private JsonObject getJsonObject(Gson gson, String log) throws FileNotFoundException {
		JsonElement json = gson.fromJson(new FileReader(log), JsonElement.class);
		return json.getAsJsonObject();
	}

	public static String anonymize(String sourceString) {
		String result = replaceTextOfMatchGroup(sourceString, ".*'INSTANCE': '(\\w+)'.*", world -> "my_instance");
		result = replaceTextOfMatchGroup(result, ".*/tmp/(\\w+)/.*", world -> "my_tmp");
		result = replaceTextOfMatchGroup(result, ".*'INSTANCES': '(\\w+)'.*", world -> "instances");
		result = replaceTextOfMatchGroup(result, ".*instance (\\w+).*", world -> "my_instance");
		result = replaceTextOfMatchGroup(result, ".*'SOURCE_INSTANCES': '(.+)'.*", world -> "my_source_instances");
		result = replaceTextOfMatchGroup(result, ".*'SOURCE_INSTANCE': '(\\w+)'.*", world -> "my_source_instance");
		result = replaceTextOfMatchGroup(result, ".*'TARGET_INSTANCES': '(.+)'.*", world -> "my_target_instances");
		result = replaceTextOfMatchGroup(result, ".*'TARGET_INSTANCE': '(\\w+)'.*", world -> "my_target_instance");
		return result;
	}

	public static String replaceTextOfMatchGroup(String sourceString, String regex,
			Function<String, String> replaceStrategy) {
		Pattern pattern = Pattern.compile(regex);
		int groupToReplace = 1;
		Stack<Integer> startPositions = new Stack<>();
		Stack<Integer> endPositions = new Stack<>();
		Matcher matcher = pattern.matcher(sourceString);

		while (matcher.find()) {
			startPositions.push(matcher.start(groupToReplace));
			endPositions.push(matcher.end(groupToReplace));
		}
		StringBuilder sb = new StringBuilder(sourceString);
		while (!startPositions.isEmpty()) {
			int start = startPositions.pop();
			int end = endPositions.pop();
			if (start >= 0 && end >= 0) {
				sb.replace(start, end, replaceStrategy.apply(sourceString.substring(start, end)));
			}
		}
		return sb.toString();
	}
}
