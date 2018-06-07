package alien4cloud.paas.cloudify3.service;

import alien4cloud.paas.cloudify3.event.CloudifyManagerSnapshoted;
import alien4cloud.paas.cloudify3.event.CloudifyManagerUnreachable;
import alien4cloud.paas.cloudify3.event.DeploymentRegisteredEvent;
import alien4cloud.paas.cloudify3.model.Execution;
import alien4cloud.paas.cloudify3.model.GetBackendExecutionsResult;
import alien4cloud.paas.cloudify3.restclient.ExecutionBackendClient;
import alien4cloud.paas.cloudify3.service.model.CloudifySnapshot;
import alien4cloud.paas.cloudify3.util.SyspropConfig;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * In charge of snapshoting cfy status at startup, and feed a4c status in consequences.
 */
//@Service("cloudify-snapshot-service")
@Slf4j
public class SnapshotService {

    /**
     * Since the query to get execution is a GET, we need to query per batch to avoid HTTP 414 Request-URI Too Large errors.
     */
    private static final int BATCH_SIZE = 100;

    @Resource
    private ExecutionBackendClient executionBackendClient;

    @Resource(name = "cloudify-async-thread-pool")
    private ThreadPoolTaskExecutor restPool;

    @Resource(name = "cloudify-scheduler")
    private ListeningScheduledExecutorService scheduler;

    @Autowired
    private ApplicationEventPublisher bus;

    private final Set<String> registeredDeploymentIds = Sets.newHashSet();
    private final ReentrantReadWriteLock registryLock = new ReentrantReadWriteLock();

    public void init() {
        long snapshotPollPeriod = SyspropConfig.getLong(SyspropConfig.SNAPSHOT_TRIGGER_PERIOD_IN_SECONDS, 5);
        log.info("Will snapshot cfy each {} seconds", snapshotPollPeriod);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                snapshotCloudify();
            } catch (Exception e) {
                log.error("Fatal error occurred: ", e);
            }
        }, 0, snapshotPollPeriod, TimeUnit.SECONDS);
    }

    @EventListener
    public void registerDeployments(DeploymentRegisteredEvent e) {
        registryLock.writeLock().lock();
        try {
            for (String deploymentId : e.getDeploymentIds()) {
                registeredDeploymentIds.add(deploymentId);
            }
        } finally {
            registryLock.writeLock().unlock();
        }
    }

    /**
     * Snapshots the state of cfy for other services.
     */
    public void snapshotCloudify() {

        log.debug("Snapshoting cfy");

        String[] registeredDeploymentIdArray = null;
        // acquire the lock just to read the Set
        registryLock.readLock().lock();
        try {
            registeredDeploymentIdArray = registeredDeploymentIds.toArray(new String[0]);
        } finally {
            // we can now release the lock
            registryLock.readLock().unlock();
        }

        int deploymentIdsCount = registeredDeploymentIdArray.length;

        if (deploymentIdsCount == 0) {
            log.trace("No registered deployments to snapshot");
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("About to query for executions concerning {} deployments", deploymentIdsCount);
        }

        // now let's iterate to query per batch
        // a map of deploymentId -> excutions
        Map<String, List<Execution>> executionsPerDeployment = Maps.newHashMap();
        int _executionCount = 0;

        int lastIterationIdx = deploymentIdsCount / BATCH_SIZE;

        for (int i=0; i <= lastIterationIdx; i++) {
            int querySize = BATCH_SIZE;
            if (i == lastIterationIdx) {
                querySize = deploymentIdsCount % BATCH_SIZE;
                if (querySize == 0) {
                    // no more batch to fetch
                    break;
                }
            }

            int offset = i * BATCH_SIZE;
            String[] requestDeploymentIds = Arrays.copyOfRange(registeredDeploymentIdArray, offset, offset + querySize);

            if (log.isTraceEnabled()) {
                log.trace("Querying for execution using deployment_id from {} to {} : {}", offset, offset + querySize, requestDeploymentIds);
            }

            try {
                GetBackendExecutionsResult backendExecutionsResult = executionBackendClient.asyncList(requestDeploymentIds).get();
                for (Execution execution : backendExecutionsResult.getItems()) {
                    List<Execution> deploymentExecution = executionsPerDeployment.get(execution.getDeploymentId());
                    if (deploymentExecution == null) {
                        deploymentExecution = Lists.newArrayList();
                        executionsPerDeployment.put(execution.getDeploymentId(), deploymentExecution);
                    }
                    _executionCount += deploymentExecution.size();
                    deploymentExecution.add(execution);
                }
            } catch (Exception e) {
                if (requestDeploymentIds.length == 1 && e.getCause() instanceof HttpClientErrorException && ((HttpClientErrorException)e.getCause()).getRawStatusCode() == 404) {
                    // in this particular case, we only have requested a single deployment_id and it was not found
                    // it just means that the undeployment has been terminated
                    log.trace("No executions found for deployment " + requestDeploymentIds[0]);
                } else {
                    log.warn("An exception occured while snapshoting cfy ({}), will be reattempted in next schedule", e.getMessage());
                    if (log.isDebugEnabled()) {
                        log.debug("Exception was :", e);
                    }
                    bus.publishEvent(new CloudifyManagerUnreachable(this));
                    // very important to break here, if we fail on one of the queries we must stop the batch iteration
                    // if we don't break, the risk is to publish an incomplete snapshot
                    break;
                }
            }
        }
        if (executionsPerDeployment.size() == 0) {
            log.debug("No executions found on cfy");
        } else {
            if (log.isDebugEnabled()) {
                log.debug("About to publish cfy snapshot containing {} excecutions / {} deployments", _executionCount, executionsPerDeployment.size());
            }
            bus.publishEvent(new CloudifyManagerSnapshoted(this, new CloudifySnapshot(executionsPerDeployment)));
        }
    }

}
