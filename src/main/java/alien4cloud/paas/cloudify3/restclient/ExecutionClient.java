package alien4cloud.paas.cloudify3.restclient;

import java.util.Map;

import javax.inject.Inject;

import lombok.Setter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;

import alien4cloud.paas.cloudify3.configuration.CloudConfigurationHolder;
import alien4cloud.paas.cloudify3.model.Execution;
import alien4cloud.paas.cloudify3.util.FutureUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ExecutionClient extends AbstractClient {
    public static final String EXECUTIONS_PATH = "/executions";

    @Inject
    @Setter
    private CloudConfigurationHolder configurationHolder;

    @Override
    protected String getPath() {
        return EXECUTIONS_PATH;
    }

    public ListenableFuture<Execution[]> asyncList(String deploymentId, boolean includeSystemWorkflow) {
        if (log.isDebugEnabled()) {
            log.debug("List execution");
        }
        if (deploymentId != null && deploymentId.length() > 0) {
            return FutureUtil.unwrapRestResponse(getForEntity(getBaseUrl(getManagerUrl(), "deployment_id", "include_system_workflows"), Execution[].class,
                    deploymentId, includeSystemWorkflow));
        } else {
            return FutureUtil.unwrapRestResponse(getForEntity(getBaseUrl(getManagerUrl()), Execution[].class));
        }
    }

    @SneakyThrows
    public Execution[] list(String deploymentId, boolean includeSystemWorkflow) {
        return asyncList(deploymentId, includeSystemWorkflow).get();
    }

    public ListenableFuture<Execution> asyncStart(String deploymentId, String workflowId, Map<String, ?> parameters, boolean allowCustomParameters,
            boolean force) {
        if (log.isDebugEnabled()) {
            log.debug("Start execution of workflow {} for deployment {}", workflowId, deploymentId);
        }
        Map<String, Object> request = Maps.newHashMap();
        request.put("deployment_id", deploymentId);
        request.put("workflow_id", workflowId);
        request.put("parameters", parameters);
        request.put("allow_custom_parameters", allowCustomParameters);
        request.put("force", force);
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return FutureUtil.unwrapRestResponse(exchange(getBaseUrl(getManagerUrl()), HttpMethod.POST, new HttpEntity<>(request, headers), Execution.class));
    }

    @SneakyThrows
    public Execution start(String deploymentId, String workflowId, Map<String, Object> parameters, boolean allowCustomParameters, boolean force) {
        return asyncStart(deploymentId, workflowId, parameters, allowCustomParameters, force).get();
    }

    public ListenableFuture<Execution> asyncCancel(String id, boolean force) {
        if (log.isDebugEnabled()) {
            log.debug("Cancel execution {}", id);
        }
        Map<String, Object> request = Maps.newHashMap();
        if (force) {
            request.put("action", "force-cancel");
        } else {
            request.put("action", "cancel");
        }
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return FutureUtil.unwrapRestResponse(
                exchange(getSuffixedUrl(getManagerUrl(), "/{id}"), HttpMethod.POST, new HttpEntity<>(request, headers), Execution.class, id));
    }

    @SneakyThrows
    public Execution cancel(String id, boolean force) {
        return asyncCancel(id, force).get();
    }

    public ListenableFuture<Execution> asyncRead(String id) {
        if (log.isDebugEnabled()) {
            log.debug("Read execution {}", id);
        }
        return FutureUtil.unwrapRestResponse(getForEntity(getSuffixedUrl(getManagerUrl(), "/{id}"), Execution.class, id));
    }

    @SneakyThrows
    public Execution read(String id) {
        return asyncRead(id).get();
    }

    private String getManagerUrl() {
        return configurationHolder.getConfiguration().getUrl();
    }
}
