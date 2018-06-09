package alien4cloud.paas.cloudify3.loganalyser;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by xdegenne on 09/06/2018.
 */
public class LogUtils {

    public static String anonymize(String sourceString) {
        String result = sourceString;
        result = StringUtils.replaceAll(result, "Mock\\_(\\w+)", "Mock_?");
        result = StringUtils.replaceAll(result, "\\/tmp\\/(.+)\\/", "/tmp/?/");
        result = StringUtils.replaceAll(result, "tmp\\/(\\w+)\\/", "/tmp/?/");
        result = StringUtils.replaceAll(result, "Removing directory: /tmp/(.+)", "Removing directory: /tmp/?");
        result = StringUtils.replaceAll(result, "Extracting archive: (.+)", "Extracting archive: http://?");
        result = StringUtils.replaceAll(result, "Deleting deployment (.+) environment", "Deleting deployment ? environment");
        return result;
    }

}
