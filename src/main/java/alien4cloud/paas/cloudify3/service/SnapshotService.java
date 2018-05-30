package alien4cloud.paas.cloudify3.service;

import alien4cloud.paas.cloudify3.configuration.CloudConfigurationHolder;
import alien4cloud.paas.cloudify3.event.CloudifySnapshotReceived;
import alien4cloud.paas.cloudify3.restclient.DeploymentClient;
import alien4cloud.paas.cloudify3.restclient.ExecutionClient;
import alien4cloud.paas.cloudify3.restclient.NodeClient;
import alien4cloud.paas.cloudify3.restclient.NodeInstanceClient;
import alien4cloud.paas.cloudify3.service.model.CloudifySnapshot;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * In charge of snapshoting cfy status at startup, and feed a4c status in consequences.
 */
@org.springframework.stereotype.Service("cloudify-snapshot-service")
@Slf4j
public class SnapshotService {

    @Resource
    private ExecutionClient executionClient;

    @Resource
    private NodeInstanceClient nodeInstanceClient;

    @Resource
    private NodeClient nodeClient;

    @Resource
    private DeploymentClient deploymentClient;

    @Resource
    private CloudConfigurationHolder cloudConfigurationHolder;

    @Resource
    private RuntimePropertiesService runtimePropertiesService;

    @Resource(name = "cloudify-async-thread-pool")
    private ThreadPoolTaskExecutor restPool;

    @Resource(name="cloudify-scheduler")
    private ListeningScheduledExecutorService scheduler;

    @Autowired
    private ApplicationEventPublisher bus;

    /**
     * Snapshots the state of cfy for other services.
     */
    @PostConstruct
    public void init() {

        CloudifySnapshot cloudifySnapshot = new CloudifySnapshot();
        // TODO: Do the necessary stuff ...
        // deploymentClient.asyncList() then executionClient.asyncList()

        // better call this asynchronously
        bus.publishEvent(new CloudifySnapshotReceived(this, cloudifySnapshot));
    }


}
