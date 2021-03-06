package alien4cloud.paas.cloudify3.shared.restclient;

import java.util.Map;

import alien4cloud.paas.cloudify3.model.ListResponse;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;

import alien4cloud.paas.cloudify3.model.Execution;
import alien4cloud.paas.cloudify3.model.ListExecutionResponse;
import alien4cloud.paas.cloudify3.util.FutureUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExecutionClient {
    private static final String EXECUTIONS_PATH = "/api/v3/executions";
    private static final String ID_EXECUTIONS_PATH = EXECUTIONS_PATH + "/{id}";
    private final ApiHttpClient client;

    public ExecutionClient(ApiHttpClient apiHttpClient) {
        this.client = apiHttpClient;
    }

    private ListenableFuture<Execution[]> unwrapListResponse(ListenableFuture<ListExecutionResponse> listExecutionResponse) {
        return Futures.transform(listExecutionResponse, (Function<ListExecutionResponse, Execution[]>) ListResponse::getItems);
    }

    public ListenableFuture<Execution[]> asyncList(String deploymentId, boolean includeSystemWorkflow) {
        if (log.isDebugEnabled()) {
            log.debug("List execution");
        }
        if (deploymentId != null && deploymentId.length() > 0) {
            return unwrapListResponse(
                    FutureUtil.unwrapRestResponse(client.getForEntity(client.buildRequestUrl(EXECUTIONS_PATH, "deployment_id", "_include_system_workflows"),
                            ListExecutionResponse.class, deploymentId, includeSystemWorkflow)));
        } else {
            return unwrapListResponse(FutureUtil.unwrapRestResponse(client.getForEntity(client.buildRequestUrl(EXECUTIONS_PATH), ListExecutionResponse.class)));
        }
    }

    @SneakyThrows
    public Execution[] list(String deploymentId, boolean includeSystemWorkflow) {
        // Note that there should not be many open executions for a given deployment so we don't use pagination parameters.
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
        return FutureUtil.unwrapRestResponse(
                client.exchange(client.buildRequestUrl(EXECUTIONS_PATH), HttpMethod.POST, new HttpEntity<>(request, headers), Execution.class));
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
                client.exchange(client.buildRequestUrl(ID_EXECUTIONS_PATH), HttpMethod.POST, new HttpEntity<>(request, headers), Execution.class, id));
    }

    @SneakyThrows
    public Execution cancel(String id, boolean force) {
        return asyncCancel(id, force).get();
    }

    public ListenableFuture<Execution> asyncRead(String id) {
        if (log.isDebugEnabled()) {
            log.debug("Read execution {}", id);
        }
        return FutureUtil.unwrapRestResponse(client.getForEntity(client.buildRequestUrl(ID_EXECUTIONS_PATH), Execution.class, id));
    }

    @SneakyThrows
    public Execution read(String id) {
        return asyncRead(id).get();
    }
}
