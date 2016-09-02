package alien4cloud.paas.cloudify3.service;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.collections4.MapUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.model.topology.NodeTemplate;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.cloudify3.configuration.CloudConfigurationHolder;
import alien4cloud.paas.cloudify3.configuration.MappingConfigurationHolder;
import alien4cloud.paas.cloudify3.model.AbstractCloudifyModel;
import alien4cloud.paas.cloudify3.model.Deployment;
import alien4cloud.paas.cloudify3.model.Execution;
import alien4cloud.paas.cloudify3.model.ExecutionStatus;
import alien4cloud.paas.cloudify3.model.Node;
import alien4cloud.paas.cloudify3.model.NodeInstance;
import alien4cloud.paas.cloudify3.model.NodeInstanceStatus;
import alien4cloud.paas.cloudify3.model.Workflow;
import alien4cloud.paas.cloudify3.restclient.DeploymentClient;
import alien4cloud.paas.cloudify3.restclient.ExecutionClient;
import alien4cloud.paas.cloudify3.restclient.NodeClient;
import alien4cloud.paas.cloudify3.restclient.NodeInstanceClient;
import alien4cloud.paas.cloudify3.util.DateUtil;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.InstanceInformation;
import alien4cloud.paas.model.InstanceStatus;
import alien4cloud.paas.model.PaaSDeploymentStatusMonitorEvent;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import alien4cloud.utils.MapUtil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.FutureFallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

/**
 * Handle all deployment status request
 */
@Component("cloudify-status-service")
@Slf4j
public class StatusService {

    private Map<String, DeploymentStatus> statusCache = Maps.newHashMap();

    private ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    @Resource
    private EventService eventService;

    @Resource
    private ExecutionClient executionDAO;

    @Resource
    private NodeInstanceClient nodeInstanceDAO;

    @Resource
    private NodeClient nodeDAO;

    @Resource
    private DeploymentClient deploymentDAO;

    @Resource
    private CloudConfigurationHolder cloudConfigurationHolder;

    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;

    @Resource
    private MappingConfigurationHolder mappingConfigurationHolder;

    @Resource
    private RuntimePropertiesService runtimePropertiesService;

    @Resource
    private ListeningScheduledExecutorService scheduler;

    public void scheduleRefreshStatus(final String deploymentPaaSId) {
        scheduleRefreshStatus(deploymentPaaSId, statusCache.get(deploymentPaaSId));
    }

    private void scheduleRefreshStatus(final String deploymentPaaSId, final DeploymentStatus currentStatus) {
        long scheduleTime;
        switch (currentStatus) {
        case DEPLOYMENT_IN_PROGRESS:
        case UNDEPLOYMENT_IN_PROGRESS:
            // Poll more aggressively if deployment in progress or undeployment in progress
            scheduleTime = cloudConfigurationHolder.getConfiguration().getDelayBetweenInProgressDeploymentStatusPolling();
            break;
        default:
            scheduleTime = cloudConfigurationHolder.getConfiguration().getDelayBetweenDeploymentStatusPolling();
            break;
        }
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                if (log.isDebugEnabled()) {
                    log.debug("Running refresh state for {} with current state {}", deploymentPaaSId, currentStatus);
                }
                try {
                    cacheLock.readLock().lock();
                    // It means someone cleaned entry before the scheduled task run
                    if (!statusCache.containsKey(deploymentPaaSId)) {
                        return;
                    }
                } finally {
                    cacheLock.readLock().unlock();
                }
                ListenableFuture<DeploymentStatus> newStatusFuture = asyncGetStatus(deploymentPaaSId);
                Function<DeploymentStatus, DeploymentStatus> newStatusAdapter = new Function<DeploymentStatus, DeploymentStatus>() {
                    @Override
                    public DeploymentStatus apply(DeploymentStatus newStatus) {
                        registerDeploymentStatusAndReschedule(deploymentPaaSId, newStatus);
                        return newStatus;
                    }
                };
                ListenableFuture<DeploymentStatus> refreshFuture = Futures.transform(newStatusFuture, newStatusAdapter);
                Futures.addCallback(refreshFuture, new FutureCallback<DeploymentStatus>() {
                    @Override
                    public void onSuccess(DeploymentStatus result) {
                        if (log.isDebugEnabled()) {
                            log.debug("Successfully refreshed state for {} with new state {}", deploymentPaaSId, result);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.warn("Failed to refresh state for " + deploymentPaaSId, t);
                    }
                });

            }
        }, scheduleTime, TimeUnit.SECONDS);
    }

    public void init(Map<String, PaaSTopologyDeploymentContext> activeDeploymentContexts) {
        for (Map.Entry<String, PaaSTopologyDeploymentContext> contextEntry : activeDeploymentContexts.entrySet()) {
            String deploymentPaaSId = contextEntry.getKey();
            // Try to retrieve the last deployment status event to initialize the cache
            Map<String, String[]> filters = Maps.newHashMap();
            filters.put("deploymentId", new String[] { contextEntry.getValue().getDeploymentId() });
            GetMultipleDataResult<PaaSDeploymentStatusMonitorEvent> lastEventResult = alienMonitorDao.search(PaaSDeploymentStatusMonitorEvent.class, null,
                    filters, null, null, 0, 1, "date", true);
            if (lastEventResult.getData() != null && lastEventResult.getData().length > 0) {
                statusCache.put(deploymentPaaSId, lastEventResult.getData()[0].getDeploymentStatus());
            }
            // Query the manager to be sure that the status has not changed
            DeploymentStatus deploymentStatus;
            try {
                deploymentStatus = asyncGetStatus(deploymentPaaSId).get();
            } catch (Exception e) {
                log.error("Failed to get status of application " + deploymentPaaSId, e);
                deploymentStatus = DeploymentStatus.UNKNOWN;
            }
            registerDeploymentStatusAndReschedule(deploymentPaaSId, deploymentStatus);
        }
    }

    private ListenableFuture<DeploymentStatus> asyncGetStatus(String deploymentPaaSId) {
        ListenableFuture<Deployment> deploymentFuture = deploymentDAO.asyncRead(deploymentPaaSId);
        AsyncFunction<Deployment, Execution[]> executionsAdapter = new AsyncFunction<Deployment, Execution[]>() {
            @Override
            public ListenableFuture<Execution[]> apply(Deployment deployment) throws Exception {
                return executionDAO.asyncList(deployment.getId(), false);
            }
        };
        ListenableFuture<Execution[]> executionsFuture = Futures.transform(deploymentFuture, executionsAdapter);
        Function<Execution[], DeploymentStatus> deploymentStatusAdapter = new Function<Execution[], DeploymentStatus>() {
            @Override
            public DeploymentStatus apply(Execution[] executions) {
                return doGetStatus(executions);
            }
        };
        ListenableFuture<DeploymentStatus> statusFuture = Futures.transform(executionsFuture, deploymentStatusAdapter);
        return Futures.withFallback(statusFuture, new FutureFallback<DeploymentStatus>() {
            @Override
            public ListenableFuture<DeploymentStatus> create(Throwable throwable) throws Exception {
                // In case of error we give back unknown status and let the next polling determine the application status
                if (throwable instanceof HttpClientErrorException) {
                    if (((HttpClientErrorException) throwable).getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                        // Only return undeployed for an application if we received a 404 which means it was deleted
                        log.info("Application is not found on cloudify, it must have been deleted");
                        return Futures.immediateFuture(DeploymentStatus.UNDEPLOYED);
                    }
                }
                return Futures.immediateFuture(DeploymentStatus.UNKNOWN);
            }
        });
    }

    private DeploymentStatus doGetStatus(Execution[] executions) {
        Execution lastExecution = null;
        // Get the last install or uninstall execution, to check for status
        for (Execution execution : executions) {
            if (log.isDebugEnabled()) {
                log.debug("Deployment {} has execution {} created at {} for workflow {} in status {}", execution.getDeploymentId(), execution.getId(),
                        execution.getCreatedAt(), execution.getWorkflowId(), execution.getStatus());
            }
            Set<String> relevantExecutionsForStatus = Sets.newHashSet(Workflow.INSTALL, Workflow.DELETE_DEPLOYMENT_ENVIRONMENT,
                    Workflow.CREATE_DEPLOYMENT_ENVIRONMENT, Workflow.UNINSTALL);
            // Only consider install/uninstall workflow to check for deployment status
            if (relevantExecutionsForStatus.contains(execution.getWorkflowId())) {
                if (lastExecution == null) {
                    lastExecution = execution;
                } else if (DateUtil.compare(execution.getCreatedAt(), lastExecution.getCreatedAt()) > 0) {
                    lastExecution = execution;
                }
            }
        }
        if (lastExecution == null) {
            // No install and uninstall yet it must be deployment in progress
            return DeploymentStatus.DEPLOYMENT_IN_PROGRESS;
        } else {
            if (ExecutionStatus.isCancelled(lastExecution.getStatus())) {
                // The only moment when we cancel a running execution is when we undeploy
                return DeploymentStatus.UNDEPLOYMENT_IN_PROGRESS;
            }
            // Only consider changing state when an execution has been finished or in failure
            // Execution in cancel or starting will return null to not impact on the application state as they are intermediary state
            switch (lastExecution.getWorkflowId()) {
            case Workflow.CREATE_DEPLOYMENT_ENVIRONMENT:
                if (ExecutionStatus.isInProgress(lastExecution.getStatus()) || ExecutionStatus.isTerminatedSuccessfully(lastExecution.getStatus())) {
                    return DeploymentStatus.INIT_DEPLOYMENT;
                } else if (ExecutionStatus.isTerminatedWithFailure(lastExecution.getStatus())) {
                    return DeploymentStatus.FAILURE;
                } else {
                    return DeploymentStatus.UNKNOWN;
                }
            case Workflow.INSTALL:
                if (ExecutionStatus.isInProgress(lastExecution.getStatus())) {
                    return DeploymentStatus.DEPLOYMENT_IN_PROGRESS;
                } else if (ExecutionStatus.isTerminatedSuccessfully(lastExecution.getStatus())) {
                    return DeploymentStatus.DEPLOYED;
                } else if (ExecutionStatus.isTerminatedWithFailure(lastExecution.getStatus())) {
                    return DeploymentStatus.FAILURE;
                } else {
                    return DeploymentStatus.UNKNOWN;
                }
            case Workflow.UNINSTALL:
                if (ExecutionStatus.isInProgress(lastExecution.getStatus()) || ExecutionStatus.isTerminatedSuccessfully(lastExecution.getStatus())) {
                    return DeploymentStatus.UNDEPLOYMENT_IN_PROGRESS;
                } else if (ExecutionStatus.isTerminatedWithFailure(lastExecution.getStatus())) {
                    return DeploymentStatus.FAILURE;
                } else {
                    return DeploymentStatus.UNKNOWN;
                }
            case Workflow.DELETE_DEPLOYMENT_ENVIRONMENT:
                if (ExecutionStatus.isInProgress(lastExecution.getStatus())) {
                    return DeploymentStatus.UNDEPLOYMENT_IN_PROGRESS;
                } else if (ExecutionStatus.isTerminatedSuccessfully(lastExecution.getStatus())) {
                    return DeploymentStatus.UNDEPLOYED;
                } else if (ExecutionStatus.isTerminatedWithFailure(lastExecution.getStatus())) {
                    return DeploymentStatus.FAILURE;
                } else {
                    return DeploymentStatus.UNKNOWN;
                }
            default:
                return DeploymentStatus.UNKNOWN;
            }
        }
    }

    public DeploymentStatus getStatus(String deploymentPaaSId) {
        try {
            cacheLock.readLock().lock();
            if (!statusCache.containsKey(deploymentPaaSId)) {
                return DeploymentStatus.UNDEPLOYED;
            } else {
                return statusCache.get(deploymentPaaSId);
            }
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    public void getStatus(String deploymentPaaSId, IPaaSCallback<DeploymentStatus> callback) {
        callback.onSuccess(getStatus(deploymentPaaSId));
    }

    public void getInstancesInformation(final PaaSTopologyDeploymentContext deploymentContext,
            final IPaaSCallback<Map<String, Map<String, InstanceInformation>>> callback) {
        try {
            cacheLock.readLock().lock();
            if (!statusCache.containsKey(deploymentContext.getDeploymentPaaSId())) {
                callback.onSuccess(Maps.<String, Map<String, InstanceInformation>> newHashMap());
                return;
            }
        } finally {
            cacheLock.readLock().unlock();
        }
        ListenableFuture<NodeInstance[]> instancesFuture = nodeInstanceDAO.asyncList(deploymentContext.getDeploymentPaaSId());
        ListenableFuture<Node[]> nodesFuture = nodeDAO.asyncList(deploymentContext.getDeploymentPaaSId(), null);
        ListenableFuture<List<AbstractCloudifyModel[]>> combinedFutures = Futures.allAsList(instancesFuture, nodesFuture);
        Futures.addCallback(combinedFutures, new FutureCallback<List<AbstractCloudifyModel[]>>() {
            @Override
            public void onSuccess(List<AbstractCloudifyModel[]> nodeAndNodeInstances) {
                NodeInstance[] instances = (NodeInstance[]) nodeAndNodeInstances.get(0);
                Node[] nodes = (Node[]) nodeAndNodeInstances.get(1);
                Map<String, Node> nodeMap = Maps.newHashMap();
                for (Node node : nodes) {
                    nodeMap.put(node.getId(), node);
                }

                Map<String, NodeInstance> nodeInstanceMap = Maps.newHashMap();
                for (NodeInstance instance : instances) {
                    nodeInstanceMap.put(instance.getId(), instance);
                }

                Map<String, Map<String, InstanceInformation>> information = Maps.newHashMap();
                for (NodeInstance instance : instances) {
                    NodeTemplate nodeTemplate = deploymentContext.getDeploymentTopology().getNodeTemplates().get(instance.getNodeId());
                    if (nodeTemplate == null) {
                        // Sometimes we have generated instance that do not exist in alien topology
                        continue;
                    }
                    Map<String, InstanceInformation> nodeInformation = information.get(instance.getNodeId());
                    if (nodeInformation == null) {
                        nodeInformation = Maps.newHashMap();
                        information.put(instance.getNodeId(), nodeInformation);
                    }
                    String instanceId = instance.getId();
                    InstanceInformation instanceInformation = new InstanceInformation();
                    instanceInformation.setState(instance.getState());
                    InstanceStatus instanceStatus = NodeInstanceStatus.getInstanceStatusFromState(instance.getState());
                    if (instanceStatus == null) {
                        continue;
                    } else {
                        instanceInformation.setInstanceStatus(instanceStatus);
                    }
                    Map<String, String> runtimeProperties = null;
                    try {
                        runtimeProperties = MapUtil.toString(instance.getRuntimeProperties());
                    } catch (JsonProcessingException e) {
                        log.error("Unable to stringify runtime properties", e);
                    }
                    instanceInformation.setRuntimeProperties(runtimeProperties);
                    Node node = nodeMap.get(instance.getNodeId());
                    if (node != null && runtimeProperties != null) {
                        instanceInformation.setAttributes(runtimePropertiesService.getAttributes(node, instance, nodeMap, nodeInstanceMap));
                    }
                    nodeInformation.put(instanceId, instanceInformation);
                }
                String floatingIpPrefix = mappingConfigurationHolder.getMappingConfiguration().getGeneratedNodePrefix() + "_floating_ip_";
                for (NodeInstance instance : instances) {
                    if (instance.getId().startsWith(floatingIpPrefix)) {
                        // It's a floating ip then must fill the compute with public ip address
                        String computeNodeId = instance.getNodeId().substring(floatingIpPrefix.length());
                        Map<String, InstanceInformation> computeNodeInformation = information.get(computeNodeId);
                        if (MapUtils.isNotEmpty(computeNodeInformation)) {
                            InstanceInformation firstComputeInstanceFound = computeNodeInformation.values().iterator().next();
                            firstComputeInstanceFound.getAttributes().put("public_ip_address",
                                    String.valueOf(instance.getRuntimeProperties().get("floating_ip_address")));
                        }
                    }
                }
                callback.onSuccess(information);
            }

            @Override
            public void onFailure(Throwable t) {
                if (log.isDebugEnabled()) {
                    log.debug("Problem retrieving instance information for deployment <" + deploymentContext.getDeploymentPaaSId() + "> ");
                }
                callback.onSuccess(Maps.<String, Map<String, InstanceInformation>> newHashMap());
            }
        });
    }

    /**
     * Register for the first time a deployment with a status to the cache
     * 
     * @param deploymentPaaSId the deployment id
     */
    public void registerDeployment(String deploymentPaaSId) {
        try {
            cacheLock.writeLock().lock();
            statusCache.put(deploymentPaaSId, DeploymentStatus.INIT_DEPLOYMENT);
            eventService.registerDeploymentEvent(deploymentPaaSId, DeploymentStatus.INIT_DEPLOYMENT);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Register a new deployment status of an existing deployment
     *
     * @param deploymentPaaSId the deployment id
     * @param newDeploymentStatus the new deployment status
     */
    public void registerDeploymentStatus(String deploymentPaaSId, DeploymentStatus newDeploymentStatus) {
        try {
            cacheLock.writeLock().lock();
            if (DeploymentStatus.UNDEPLOYED.equals(newDeploymentStatus)) {
                // Application has been removed, don't need to monitor it anymore
                statusCache.remove(deploymentPaaSId);
                eventService.registerDeploymentEvent(deploymentPaaSId, DeploymentStatus.UNDEPLOYED);
            } else {
                DeploymentStatus deploymentStatus = statusCache.get(deploymentPaaSId);
                if (!newDeploymentStatus.equals(deploymentStatus)) {
                    // Deployment status has changed
                    statusCache.put(deploymentPaaSId, newDeploymentStatus);
                    if (!DeploymentStatus.UNKNOWN.equals(newDeploymentStatus)) {
                        // Send back event to Alien only if it's a known status
                        eventService.registerDeploymentEvent(deploymentPaaSId, newDeploymentStatus);
                    }
                }
            }
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    private void registerDeploymentStatusAndReschedule(String deploymentPaaSId, DeploymentStatus newDeploymentStatus) {
        registerDeploymentStatus(deploymentPaaSId, newDeploymentStatus);
        if (!DeploymentStatus.UNDEPLOYED.equals(newDeploymentStatus)) {
            scheduleRefreshStatus(deploymentPaaSId, newDeploymentStatus);
        }
    }
}
