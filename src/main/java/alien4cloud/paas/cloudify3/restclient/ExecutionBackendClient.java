package alien4cloud.paas.cloudify3.restclient;

import alien4cloud.paas.cloudify3.configuration.CloudConfigurationHolder;
import alien4cloud.paas.cloudify3.model.Execution;
import alien4cloud.paas.cloudify3.model.GetBackendExecutionsResult;
import alien4cloud.paas.cloudify3.util.FutureUtil;
import com.google.common.collect.Maps;
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
        Map<String, Object> request = Maps.newHashMap();
        request.put("deployment_id", deploymentIds);
        request.put("_include", new String[]{"id", "workflow_id", "status", "deployment_id"});
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

        return FutureUtil.unwrapRestResponse(getForEntity(getBaseUrl(getManagerUrl()), GetBackendExecutionsResult.class, request));
    }

    private String getManagerUrl() {
        return configurationHolder.getConfiguration().getUrl();
    }
}
