package alien4cloud.paas.cloudify3.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Resource;

import org.alien4cloud.tosca.model.templates.NodeTemplate;
import org.alien4cloud.tosca.normative.ToscaNormativeUtil;
import org.alien4cloud.tosca.utils.ToscaTypeUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.cloudify3.configuration.CfyConnectionManager;
import alien4cloud.paas.cloudify3.configuration.MappingConfigurationHolder;
import alien4cloud.paas.cloudify3.error.CloudifyAPIException;
import alien4cloud.paas.cloudify3.model.AbstractCloudifyModel;
import alien4cloud.paas.cloudify3.model.Deployment;
import alien4cloud.paas.cloudify3.model.Execution;
import alien4cloud.paas.cloudify3.model.ExecutionStatus;
import alien4cloud.paas.cloudify3.model.Node;
import alien4cloud.paas.cloudify3.model.NodeInstance;
import alien4cloud.paas.cloudify3.model.NodeInstanceStatus;
import alien4cloud.paas.cloudify3.model.Workflow;
import alien4cloud.paas.cloudify3.service.event.EventService;
import alien4cloud.paas.cloudify3.util.DateUtil;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.InstanceInformation;
import alien4cloud.paas.model.InstanceStatus;
import alien4cloud.paas.model.PaaSDeploymentStatusMonitorEvent;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import alien4cloud.rest.utils.JsonUtil;
import alien4cloud.utils.MapUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Handle all deployment status request
 */
@Component("cloudify-status-service")
@Slf4j
public class StatusService {

    private Map<String, DeploymentStatus> statusCache = Maps.newHashMap();

    private Map<String, ListenableScheduledFuture<?>> statusRefreshJobs = Maps.newHashMap();

    private ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    @Resource
    private EventService eventService;

    @Resource
    private CfyConnectionManager cloudConfigurationHolder;

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
        Runnable job = () -> {
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
            Function<DeploymentStatus, DeploymentStatus> newStatusAdapter = newStatus -> {
                registerDeploymentStatusAndReschedule(deploymentPaaSId, newStatus);
                return newStatus;
            };
            ListenableFuture<DeploymentStatus> refreshFuture = Futures.transform(newStatusFuture, newStatusAdapter::apply);
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
        };
        try {
            cacheLock.writeLock().lock();
            if (!isApplicationMonitored(deploymentPaaSId)) {
                // Don't relaunch a schedule if one has been configured
                statusRefreshJobs.put(deploymentPaaSId, scheduler.schedule(job, scheduleTime, TimeUnit.SECONDS));
            } else {
                log.info("Application " + deploymentPaaSId + " has already been monitored, ignore scheduling request");
            }
        } finally {
            cacheLock.writeLock().unlock();
        }
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
            getStatusFromCloudify(deploymentPaaSId);
        }
    }

    private ListenableFuture<DeploymentStatus> asyncGetStatus(String deploymentPaaSId) {
        ListenableFuture<Deployment> deploymentFuture = cloudConfigurationHolder.getApiClient().getDeploymentClient().asyncRead(deploymentPaaSId);
        AsyncFunction<Deployment, Execution[]> executionsAdapter = deployment -> cloudConfigurationHolder.getApiClient().getExecutionClient()
                .asyncList(deployment.getId(), false);
        ListenableFuture<Execution[]> executionsFuture = Futures.transform(deploymentFuture, executionsAdapter);
        Function<Execution[], DeploymentStatus> deploymentStatusAdapter = this::doGetStatus;
        ListenableFuture<DeploymentStatus> statusFuture = Futures.transform(executionsFuture, deploymentStatusAdapter);
        return Futures.withFallback(statusFuture, throwable -> {
            // In case of error we give back unknown status and let the next polling determine the application status
            if (throwable instanceof CloudifyAPIException) {
                if (Objects.equals(HttpStatus.NOT_FOUND, ((CloudifyAPIException) throwable).getStatusCode())) {
                    // Only return undeployed for an application if we received a 404 which means it was deleted
                    log.info("Application " + deploymentPaaSId + " is not found on cloudify");
                    return Futures.immediateFuture(DeploymentStatus.UNDEPLOYED);
                }
            }
            log.warn("Unable to retrieve status for application " + deploymentPaaSId + ", its status will pass to " + DeploymentStatus.UNKNOWN, throwable);
            return Futures.immediateFuture(DeploymentStatus.UNKNOWN);
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
                    Workflow.CREATE_DEPLOYMENT_ENVIRONMENT, Workflow.UNINSTALL, Workflow.UPDATE_DEPLOYMENT);
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
            case Workflow.UPDATE_DEPLOYMENT:
                if (ExecutionStatus.isInProgress(lastExecution.getStatus())) {
                    return DeploymentStatus.UPDATE_IN_PROGRESS;
                } else if (ExecutionStatus.isTerminatedSuccessfully(lastExecution.getStatus())) {
                    return DeploymentStatus.UPDATED;
                } else if (ExecutionStatus.isTerminatedWithFailure(lastExecution.getStatus())) {
                    return DeploymentStatus.UPDATE_FAILURE;
                } else {
                    return DeploymentStatus.UNKNOWN;
                }
            default:
                return DeploymentStatus.UNKNOWN;
            }
        }
    }

    /**
     * Get status from local in memory cache
     * 
     * @param deploymentPaaSId the deployment id
     * @return status of the deployment uniquely from the in memory cache
     */
    private DeploymentStatus getStatusFromCache(String deploymentPaaSId) {
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

    /**
     * Get fresh status, only INIT_DEPLOYMENT will be sent back from cache, else retrieve the status from cloudify.
     * This is called to secure deploy and undeploy, to be sure that we have the most fresh status and not the one from cache
     * 
     * @param deploymentPaaSId id of the deployment to check
     * @return the deployment status
     */
    public DeploymentStatus getFreshStatus(String deploymentPaaSId) {
        try {
            cacheLock.readLock().lock();
            DeploymentStatus statusFromCache = getStatusFromCache(deploymentPaaSId);
            if (DeploymentStatus.INIT_DEPLOYMENT == statusFromCache) {
                // The deployment is being created
                return statusFromCache;
            }
        } finally {
            cacheLock.readLock().unlock();
        }
        // This will refresh the status of the application from cloudify
        return getStatusFromCloudify(deploymentPaaSId);
    }

    /**
     * Get the status from cloudify by querying the manager. If the deployment not found in the cache then will begin to monitor it.
     * 
     * @param deploymentPaaSId the deployment id
     * @return status retrieved from cloudify
     */
    public DeploymentStatus getStatusFromCloudify(String deploymentPaaSId) {
        DeploymentStatus deploymentStatus;
        try {
            deploymentStatus = asyncGetStatus(deploymentPaaSId).get();
        } catch (Exception e) {
            log.error("Failed to get status of application " + deploymentPaaSId, e);
            deploymentStatus = DeploymentStatus.UNKNOWN;
        }
        registerDeploymentStatusAndReschedule(deploymentPaaSId, deploymentStatus);
        return deploymentStatus;
    }

    /**
     * This is used to retrieve the status of the application, get from cache for monitored entries else get from cloudify
     * 
     * @param deploymentPaaSId the deployment id
     * @param callback the callback when the status is ready
     */
    public void getStatus(String deploymentPaaSId, IPaaSCallback<DeploymentStatus> callback) {
        try {
            cacheLock.readLock().lock();
            DeploymentStatus statusFromCache = getStatusFromCache(deploymentPaaSId);
            if (DeploymentStatus.INIT_DEPLOYMENT == statusFromCache || isApplicationMonitored(deploymentPaaSId)) {
                // The deployment is being created means that currently it's not monitored, it's in transition
                // The deployment is currently monitored so the cache can be used
                callback.onSuccess(statusFromCache);
            } else {
                Futures.addCallback(asyncGetStatus(deploymentPaaSId), new FutureCallback<DeploymentStatus>() {
                    @Override
                    public void onSuccess(DeploymentStatus newDeploymentStatus) {
                        try {
                            cacheLock.readLock().lock();
                            DeploymentStatus statusFromCache = getStatusFromCache(deploymentPaaSId);
                            if (DeploymentStatus.INIT_DEPLOYMENT == statusFromCache || isApplicationMonitored(deploymentPaaSId)) {
                                callback.onSuccess(statusFromCache);
                                // It's from cache nothing else to do
                                return;
                            } else {
                                callback.onSuccess(newDeploymentStatus);
                                // Will continue to registerDeploymentStatusAndReschedule
                            }
                        } finally {
                            cacheLock.readLock().unlock();
                        }
                        try {
                            cacheLock.writeLock().lock();
                            // A deployment has kicked in in concurrence, tricky situation
                            if (DeploymentStatus.INIT_DEPLOYMENT != getStatusFromCache(deploymentPaaSId)) {
                                doRegisterDeploymentStatus(deploymentPaaSId, newDeploymentStatus);
                                // Do not schedule anything then
                                return;
                            }
                        } finally {
                            cacheLock.writeLock().unlock();
                        }
                        if (!DeploymentStatus.UNDEPLOYED.equals(newDeploymentStatus)) {
                            scheduleRefreshStatus(deploymentPaaSId, newDeploymentStatus);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        callback.onFailure(t);
                    }
                });
            }
        } finally {
            cacheLock.readLock().unlock();
        }
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

        ListenableFuture<NodeInstance[]> instancesFuture = cloudConfigurationHolder.getApiClient().getNodeInstanceClient()
                .asyncList(deploymentContext.getDeploymentPaaSId());
        ListenableFuture<Node[]> nodesFuture = cloudConfigurationHolder.getApiClient().getNodeClient().asyncList(deploymentContext.getDeploymentPaaSId(), null);
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

                    // FIXME Workaround to handle docker/kubernetes endpoint attributes
                    Node node = nodeMap.get(instance.getNodeId());
                    if (node != null && runtimeProperties != null) {
                        Map<String, String> attributes = runtimePropertiesService.getAttributes(node, instance, nodeMap, nodeInstanceMap);
                        instanceInformation.setAttributes(attributes);
                        String masterIP = attributes.get("master_ip");
                        if (ToscaTypeUtils.isOfType(node.getType(), Lists.newArrayList(node.getTypeHierarchy()), "cloudify.kubernetes.Microservice")
                                && attributes.containsKey("service")) {
                            String serviceJson = attributes.get("service");
                            if (StringUtils.isNotBlank(serviceJson)) {
                                try {
                                    Map<String, Object> map = JsonUtil.toMap(serviceJson);
                                    String endpointPort = null;
                                    List<Object> ports = (List<Object>) map.get("ports");
                                    if (ports != null && !ports.isEmpty()) {
                                        Map<String, Object> portMap = (Map<String, Object>) (((List<Object>) map.get("ports")).get(0));
                                        if (portMap != null && portMap.get("nodePort") != null) {
                                            endpointPort = portMap.get("nodePort").toString();
                                        } else if (portMap.get("port") != null) {
                                            endpointPort = portMap.get("port").toString();
                                        }
                                    }
                                    instanceInformation.getAttributes().put("endpoint", String.format("http://%s:%s", masterIP, endpointPort));
                                    instanceInformation.getAttributes().put("endpoint_port", endpointPort);
                                    instanceInformation.getAttributes().put("endpoint_ip", masterIP);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    // FIXME End workaround

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
                callback.onSuccess(Maps.newHashMap());
            }
        });
    }

    /**
     * Register for the first time a deployment with a status to the cache
     * 
     * @param deploymentPaaSId the deployment id
     */
    public void registerDeployment(String deploymentPaaSId) {
        registerDeploymentStatus(deploymentPaaSId, DeploymentStatus.INIT_DEPLOYMENT);
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
            doRegisterDeploymentStatus(deploymentPaaSId, newDeploymentStatus);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    private void doRegisterDeploymentStatus(String deploymentPaaSId, DeploymentStatus newDeploymentStatus) {
        DeploymentStatus deploymentStatus = getStatusFromCache(deploymentPaaSId);
        if (!newDeploymentStatus.equals(deploymentStatus)) {
            // Only register event if it makes changes to the cache
            if (DeploymentStatus.UNDEPLOYED.equals(newDeploymentStatus)) {
                if (DeploymentStatus.INIT_DEPLOYMENT != deploymentStatus) {
                    // Application has been removed, don't need to monitor it anymore
                    statusCache.remove(deploymentPaaSId);
                    statusRefreshJobs.remove(deploymentPaaSId);
                    log.info("Application [" + deploymentPaaSId + "] has been undeployed");
                } else {
                    // a deployment in INIT_DEPLOYMENT must come to state DEPLOYMENT_IN_PROGRESS
                    log.info("Concurrent access to deployment [" + deploymentPaaSId + "], ignore transition from INIT_DEPLOYMENT to UNDEPLOYED");
                }
            } else {
                log.info("Application [" + deploymentPaaSId + "] passed from status " + deploymentStatus + " to " + newDeploymentStatus);
                // Deployment status has changed
                statusCache.put(deploymentPaaSId, newDeploymentStatus);
            }
            // Send back event to Alien only if it's a known status
            eventService.registerDeploymentEvent(deploymentPaaSId, newDeploymentStatus);
        }
    }

    private boolean isApplicationMonitored(String deploymentPaaSId) {
        return statusRefreshJobs.containsKey(deploymentPaaSId) && !statusRefreshJobs.get(deploymentPaaSId).isDone();
    }

    private void registerDeploymentStatusAndReschedule(String deploymentPaaSId, DeploymentStatus newDeploymentStatus) {
        registerDeploymentStatus(deploymentPaaSId, newDeploymentStatus);
        if (!DeploymentStatus.UNDEPLOYED.equals(newDeploymentStatus)) {
            scheduleRefreshStatus(deploymentPaaSId, newDeploymentStatus);
        }
    }
}
