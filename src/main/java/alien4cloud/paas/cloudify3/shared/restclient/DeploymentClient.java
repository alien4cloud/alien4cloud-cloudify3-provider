package alien4cloud.paas.cloudify3.shared.restclient;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;

import alien4cloud.paas.cloudify3.model.Deployment;
import alien4cloud.paas.cloudify3.model.ListDeploymentResponse;
import alien4cloud.paas.cloudify3.util.FutureUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeploymentClient {
    private static final String DEPLOYMENTS_PATH = "/api/v3/deployments";
    private static final String ID_DEPLOYMENTS_PATH = DEPLOYMENTS_PATH + "/{id}";
    private final ApiHttpClient client;

    public DeploymentClient(ApiHttpClient apiHttpClient) {
        this.client = apiHttpClient;
    }

    public ListenableFuture<ListDeploymentResponse> asyncList(int offset, int size) {
        if (log.isDebugEnabled()) {
            log.debug("List deployment");
        }
        return FutureUtil.unwrapRestResponse(
                client.getForEntity(client.buildRequestUrl(DEPLOYMENTS_PATH, "_offset", "_size"), ListDeploymentResponse.class, offset, size));
    }

    @SneakyThrows
    public ListDeploymentResponse list(int offset, int size) {
        return asyncList(offset, size).get();
    }

    @SneakyThrows
    public long count() {
        return client.getForEntity(client.buildRequestUrl(DEPLOYMENTS_PATH, "_offset", "_size"), ListDeploymentResponse.class, 0, 0).get().getBody()
                .getMetaData().getPagination().getTotal();
    }

    public ListenableFuture<Deployment> asyncCreate(String id, String blueprintId, Map<String, Object> inputs) {
        if (log.isDebugEnabled()) {
            log.debug("Create deployment {} for blueprint {}", id, blueprintId);
        }
        Map<String, Object> request = Maps.newHashMap();
        request.put("blueprint_id", blueprintId);
        request.put("inputs", inputs);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        return FutureUtil.unwrapRestResponse(
                client.exchange(client.buildRequestUrl(ID_DEPLOYMENTS_PATH), HttpMethod.PUT, client.createHttpEntity(request, headers), Deployment.class, id));
    }

    @SneakyThrows
    public Deployment create(String id, String blueprintId, Map<String, Object> inputs) {
        return asyncCreate(id, blueprintId, inputs).get();
    }

    public ListenableFuture<Deployment> asyncRead(String id) {
        if (log.isDebugEnabled()) {
            log.debug("Read deployment {}", id);
        }
        return FutureUtil.unwrapRestResponse(client.getForEntity(client.buildRequestUrl(ID_DEPLOYMENTS_PATH), Deployment.class, id));
    }

    @SneakyThrows
    public Deployment read(String id) {
        return asyncRead(id).get();
    }

    public ListenableFuture<?> asyncDelete(String id) {
        if (log.isDebugEnabled()) {
            log.debug("Delete deployment {}", id);
        }
        return FutureUtil.toGuavaFuture(client.delete(client.buildRequestUrl(ID_DEPLOYMENTS_PATH), id));
    }

    @SneakyThrows
    public void delete(String id) {
        asyncDelete(id).get();
    }
}
