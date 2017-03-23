package alien4cloud.paas.cloudify3.util;

import org.apache.commons.lang.StringUtils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Deprecated
public class EventUtil {

    public static String extractNodeName(String instanceId) {
        return StringUtils.substringBeforeLast(instanceId, "_");
    }
}
