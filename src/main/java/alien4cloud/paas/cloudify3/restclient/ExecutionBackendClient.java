package alien4cloud.paas.cloudify3.restclient;

import alien4cloud.paas.cloudify3.configuration.CloudConfigurationHolder;
import alien4cloud.paas.cloudify3.model.Execution;
import alien4cloud.paas.cloudify3.model.GetBackendExecutionsResult;
import alien4cloud.paas.cloudify3.util.FutureUtil;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import javax.inject.Inject;
import java.util.Map;

/**
 * Due to a Cfy bug in pagination of /deployments and /executions we must request this endpoint to snapshot Cfy.
 */
@Component
@Slf4j
public class ExecutionBackendClient extends AbstractClient {
    public static final String EXECUTIONS_PATH = "/backend/cloudify-api/executions";

    @Inject
    @Setter
    private CloudConfigurationHolder configurationHolder;

    @Override
    protected String getPath() {
        return EXECUTIONS_PATH;
    }

    public ListenableFuture<GetBackendExecutionsResult> asyncList(String[] deploymentIds) {
        if (log.isDebugEnabled()) {
            log.debug("List executions for deployments {}", deploymentIds);
        }
        Map<String, String[]> parameters = Maps.newHashMapWithExpectedSize(deploymentIds.length + 1);
        Map<String, String> parameterValues = Maps.newHashMapWithExpectedSize(deploymentIds.length + 1);
        parameters.put("_include", new String[]{"id,workflow_id,status,deployment_id"});
        parameters.put("deployment_id", deploymentIds);
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        return FutureUtil.unwrapRestResponse(getForEntity(getFullUrl(getManagerUrl(), parameters), GetBackendExecutionsResult.class));
    }

    private String getManagerUrl() {
        return configurationHolder.getConfiguration().getUrl();
    }

    /**
     * Get the url with array parameters in the form param=v1@param=v2&param=v3
     * We don't want this query to cached by RestTemplate ...
     */
    public String getFullUrl(String managerUrl, Map<String, String[]> parameters) {
        String urlPrefix = managerUrl + getPath();
        if (parameters != null && parameters.size() > 0) {
            StringBuilder urlBuilder = new StringBuilder(urlPrefix);
            if (!parameters.isEmpty()) {
                urlBuilder.append("?");
                for (Map.Entry<String, String[]> parameter : parameters.entrySet()) {
                    for (String parameterValue : parameter.getValue()) {
                        urlBuilder.append(parameter.getKey()).append("=").append(parameterValue).append("&");
                    }
                }
                // remove the last & (or ? if all parameters have 0 value)
                urlBuilder.setLength(urlBuilder.length() - 1);
            }
            return urlBuilder.toString();
        } else {
            return urlPrefix;
        }
    }

}
