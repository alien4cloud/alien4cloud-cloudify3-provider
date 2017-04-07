package alien4cloud.paas.cloudify3.restclient;

import org.springframework.stereotype.Component;

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
@Component
public class NodeInstanceClient extends AbstractClient {

    public static final String NODE_INSTANCES_PATH = "/node-instances";

    @Override
    protected String getPath() {
        return NODE_INSTANCES_PATH;
    }

    public ListenableFuture<NodeInstance[]> asyncList(String deploymentId) {
        if (log.isDebugEnabled()) {
            log.debug("List node instances for deployment {}", deploymentId);
        }
        return Futures.transform(FutureUtil.unwrapRestResponse(getForEntity(getBaseUrl("deployment_id"), ListNodeInstanceResponse.class, deploymentId)),
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
        return FutureUtil.unwrapRestResponse(getForEntity(getSuffixedUrl("/{id}"), NodeInstance.class, id));
    }

    @SneakyThrows
    public NodeInstance read(String id) {
        return asyncRead(id).get();
    }
}
