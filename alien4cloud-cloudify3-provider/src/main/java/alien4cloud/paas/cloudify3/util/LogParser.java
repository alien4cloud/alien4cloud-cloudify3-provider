package alien4cloud.paas.cloudify3.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

import org.apache.commons.lang.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.shared.model.LogBatch;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class LogParser {

	protected static String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	public static LogBatch parseLog(JSONObject root) {
		LogBatch result = new LogBatch();
		result.setId(root.getLong("id"));
		result.setEntries(parseLog(root.getJSONArray("entries")));
		return result;
	}

	/**
	 * Get sub array of json array
	 * @param array Json array
	 * @param begin Begin index
	 * @param end End index (exclusive)
	 * @return Json sub array
	 */
	private static JSONArray subArray(JSONArray array, int begin, int end) {
		JSONArray result = new JSONArray();
		Iterator<Object> iter = array.iterator();
		int current = 0;
		while (iter.hasNext()) {
			Object element = iter.next();
			if (begin <= current && current < end) {
				result.put(element);
			}
			current++;
		}
		return result;
	}

	private static Event[] parseLog(JSONArray entries) {
		try {
			return marshallLogs(entries);
		} catch (IOException e) {
			if (entries.length() <= 1) {
				if (log.isWarnEnabled()) {
					log.warn("Found a corrupted log {}", entries);
					return new Event[0];
				}
			}
			// Try to parse the logs in divide and conquer way
			JSONArray left = subArray(entries, 0, entries.length() / 2);
			JSONArray right = subArray(entries, entries.length() / 2, entries.length());
			return (Event[]) ArrayUtils.addAll(parseLog(left), parseLog(right));
		}
	}

	private static Event[] marshallLogs(JSONArray array) throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(array.toString(), Event[].class);
	}
}
