package alien4cloud.paas.cloudify3.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

import alien4cloud.paas.cloudify3.configuration.CloudConfigurationHolder;
import alien4cloud.paas.cloudify3.event.CloudifyInitiationError;
import alien4cloud.paas.cloudify3.event.CloudifySnapshotReceived;
import alien4cloud.paas.cloudify3.model.Deployment;
import alien4cloud.paas.cloudify3.model.Execution;
import alien4cloud.paas.cloudify3.restclient.DeploymentClient;
import alien4cloud.paas.cloudify3.restclient.ExecutionClient;
import alien4cloud.paas.cloudify3.restclient.NodeClient;
import alien4cloud.paas.cloudify3.restclient.NodeInstanceClient;
import alien4cloud.paas.cloudify3.service.model.CloudifySnapshot;
import lombok.extern.slf4j.Slf4j;

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

    @Resource(name = "cloudify-scheduler")
    private ListeningScheduledExecutorService scheduler;

    @Autowired
    private ApplicationEventPublisher bus;

    /**
     * Snapshots the state of cfy for other services.
     */
    public void snapshotCloudify() {
        log.info("Snapshoting cfy");

        // FIXME: please don't chain 2 calls, query alien4cloud.paas.cloudify3.restclient.ExecutionClient.asyncList(boolean)
        // we must wait for the 2 terminations to build the CloudifySnapshotReceived
        AsyncFunction<Deployment[], CloudifySnapshot> createCloudifySnapshot = new AsyncFunction<Deployment[], CloudifySnapshot>() {
            @Override
            public ListenableFuture<CloudifySnapshot> apply(Deployment[] input) throws Exception {
                Map<String, List<Execution>> map = Arrays.stream(input).collect(Collectors.toMap(Deployment::getId, d -> {
                    try {
                        Execution[] executions = executionClient.asyncList(d.getId(), true).get();
                        return Arrays.asList(executions);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }));
                return Futures.immediateFuture(new CloudifySnapshot(Arrays.asList(input), map));
            }
        };

        ListenableFuture<Deployment[]> clientsFuture = deploymentClient.asyncList();
        ListenableFuture<CloudifySnapshot> snapshot = Futures.transform(clientsFuture, createCloudifySnapshot);

        Futures.addCallback(snapshot, new FutureCallback<CloudifySnapshot>() {
            @Override
            public void onSuccess(CloudifySnapshot result) {
                bus.publishEvent(new CloudifySnapshotReceived(this, result));
            }

            @Override
            public void onFailure(Throwable t) {
                bus.publishEvent(new CloudifyInitiationError(this, t));
            }
        });
    }

}
