package alien4cloud.paas.cloudify3.restclient;

import alien4cloud.paas.cloudify3.configuration.CloudConfigurationHolder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import alien4cloud.paas.cloudify3.model.Node;
import alien4cloud.paas.cloudify3.util.FutureUtil;

import com.google.common.util.concurrent.ListenableFuture;

import javax.inject.Inject;

@Slf4j
@Component
public class NodeClient extends AbstractClient {
    public static final String NODES_PATH = "/nodes";

    @Inject
    private CloudConfigurationHolder configurationHolder;

    @Override
    protected String getPath() {
        return NODES_PATH;
    }

    public ListenableFuture<Node[]> asyncList(String deploymentId, String nodeId) {
        if (log.isDebugEnabled()) {
            log.debug("List nodes for deployment {}", deploymentId);
        }
        if (deploymentId == null || deploymentId.isEmpty()) {
            throw new IllegalArgumentException("Deployment id must not be null or empty");
        }
        if (nodeId != null) {
            return FutureUtil.unwrapRestResponse(getForEntity(getBaseUrl(getManagerUrl(),"deployment_id", "node_id"), Node[].class, deploymentId, nodeId));
        } else {
            return FutureUtil.unwrapRestResponse(getForEntity(getBaseUrl(getManagerUrl(),"deployment_id"), Node[].class, deploymentId));
        }
    }

    @SneakyThrows
    public Node[] list(String deploymentId, String nodeId) {
        return asyncList(deploymentId, nodeId).get();
    }

    private String getManagerUrl() {
        return configurationHolder.getConfiguration().getUrl();
    }
}
