package alien4cloud.paas.cloudify3.service;

import alien4cloud.paas.cloudify3.configuration.CloudConfigurationHolder;
import alien4cloud.paas.cloudify3.event.CloudifySnapshotReceived;
import alien4cloud.paas.cloudify3.model.Deployment;
import alien4cloud.paas.cloudify3.model.Execution;
import alien4cloud.paas.cloudify3.restclient.DeploymentClient;
import alien4cloud.paas.cloudify3.restclient.ExecutionClient;
import alien4cloud.paas.cloudify3.restclient.NodeClient;
import alien4cloud.paas.cloudify3.restclient.NodeInstanceClient;
import alien4cloud.paas.cloudify3.service.model.CloudifySnapshot;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * In charge of snapshoting cfy status at startup, and feed a4c status in consequences.
 */
@Service("cloudify-snapshot-service")
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

    public void init() {
        int statusPollDelay = cloudConfigurationHolder.getConfiguration().getDelayBetweenInProgressDeploymentStatusPolling();
        log.info("Will snapshot cfy each {} seconds", statusPollDelay);
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                snapshotCloudify();
            }
        }, statusPollDelay, statusPollDelay, TimeUnit.SECONDS);
    }

    /**
     * Snapshots the state of cfy for other services.
     */
    public void snapshotCloudify() {
        log.debug("Snapshoting cfy");

        Callable<CloudifySnapshot> task = new Callable<CloudifySnapshot>() {
            @Override
            public CloudifySnapshot call() throws Exception {
                Map<String, Deployment> deployments = Arrays.stream(deploymentClient.asyncList().get()).collect(Collectors.toMap(d -> d.getId(), d -> d));
                List<Execution> executions = Arrays.asList(executionClient.asyncList(true).get());
                Set<String> ids = deployments.keySet();
                Map<String, List<Execution>> map = executions.stream().filter(e -> ids.contains(e.getDeploymentId()))
                        .collect(Collectors.groupingBy(Execution::getDeploymentId, Collectors.toList()));
                return new CloudifySnapshot(deployments, map);
            }
        };
        ListenableFuture<CloudifySnapshot> future = scheduler.submit(task);

        Futures.addCallback(future, new FutureCallback<CloudifySnapshot>() {
            @Override
            public void onSuccess(CloudifySnapshot result) {
                bus.publishEvent(new CloudifySnapshotReceived(this, result));
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("Not able to snapshot Cloudify", t);
            }
        });
    }

}
