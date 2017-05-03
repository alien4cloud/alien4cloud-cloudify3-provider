package alien4cloud.paas.cloudify3.shared.restclient;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import alien4cloud.paas.cloudify3.model.ListNodeInstanceResponse;
import alien4cloud.paas.cloudify3.model.ListResponse;
import alien4cloud.paas.cloudify3.model.NodeInstance;
import alien4cloud.paas.cloudify3.util.FutureUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeInstanceClient {
    private static final String NODE_INSTANCES_PATH = "/api/v3/node-instances";
    private static final String ID_NODE_INSTANCES_PATH = NODE_INSTANCES_PATH + "/{id}";

    private final ApiHttpClient client;

    public NodeInstanceClient(ApiHttpClient apiHttpClient) {
        this.client = apiHttpClient;
    }

    public ListenableFuture<NodeInstance[]> asyncList(String deploymentId) {
        if (log.isDebugEnabled()) {
            log.debug("List node instances for deployment {}", deploymentId);
        }
        return Futures.transform(
                FutureUtil.unwrapRestResponse(
                        client.getForEntity(client.buildRequestUrl(NODE_INSTANCES_PATH, "deployment_id"), ListNodeInstanceResponse.class, deploymentId)),
                (Function<ListNodeInstanceResponse, NodeInstance[]>) ListResponse::getItems);

    }

    @SneakyThrows
    public NodeInstance[] list(String deploymentId) {
        return asyncList(deploymentId).get();
    }

    public ListenableFuture<NodeInstance> asyncRead(String id) {
        if (log.isDebugEnabled()) {
            log.debug("Read node instance {}", id);
        }
        return FutureUtil.unwrapRestResponse(client.getForEntity(client.buildRequestUrl(ID_NODE_INSTANCES_PATH), NodeInstance.class, id));
    }

    @SneakyThrows
    public NodeInstance read(String id) {
        return asyncRead(id).get();
    }
}
