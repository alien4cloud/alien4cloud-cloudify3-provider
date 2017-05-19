package alien4cloud.paas.cloudify3.service;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.inject.Inject;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.mapping.QueryHelper;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.dao.model.GetMultipleDataResult;
import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.paas.OrchestratorPluginService;
import alien4cloud.paas.cloudify3.configuration.CloudConfigurationHolder;
import alien4cloud.paas.cloudify3.model.CloudifyLifeCycle;
import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.model.EventAlienPersistent;
import alien4cloud.paas.cloudify3.model.EventAlienWorkflow;
import alien4cloud.paas.cloudify3.model.EventAlienWorkflowStarted;
import alien4cloud.paas.cloudify3.model.EventType;
import alien4cloud.paas.cloudify3.model.NodeInstance;
import alien4cloud.paas.cloudify3.model.NodeInstanceStatus;
import alien4cloud.paas.cloudify3.model.Workflow;
import alien4cloud.paas.cloudify3.restclient.NodeInstanceClient;
import alien4cloud.paas.cloudify3.shared.IEventConsumer;
import alien4cloud.paas.cloudify3.shared.model.CloudifyEvent;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.PaaSDeploymentStatusMonitorEvent;
import alien4cloud.paas.model.PaaSInstancePersistentResourceMonitorEvent;
import alien4cloud.paas.model.PaaSInstanceStateMonitorEvent;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import alien4cloud.paas.model.PaaSWorkflowMonitorEvent;
import alien4cloud.paas.model.PaaSWorkflowStepMonitorEvent;
import alien4cloud.utils.MapUtil;
import alien4cloud.utils.TypeScanner;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/**
 * Handle cloudify 3 events request
 */
@Component("cloudify-event-service")
@Slf4j
public class EventService implements IEventConsumer {
    @Resource
    private CloudConfigurationHolder configurationHolder;
    @Resource
    private NodeInstanceClient nodeInstanceClient;
    @Inject
    private OrchestratorPluginService orchestratorPluginService;
    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;
    @Inject
    private IOrchestratorPlugin orchestratorPlugin;

    private Date lastPollingDate;

    private Map<String, String> paaSDeploymentIdToAlienDeploymentIdMapping = Maps.newConcurrentMap();
    private Map<String, String> alienDeploymentIdToPaaSDeploymentIdMapping = Maps.newConcurrentMap();

    public void init(Map<String, PaaSTopologyDeploymentContext> activeDeploymentContexts) {
        for (Map.Entry<String, PaaSTopologyDeploymentContext> activeDeploymentContextEntry : activeDeploymentContexts.entrySet()) {
            paaSDeploymentIdToAlienDeploymentIdMapping.put(activeDeploymentContextEntry.getKey(), activeDeploymentContextEntry.getValue().getDeploymentId());
            alienDeploymentIdToPaaSDeploymentIdMapping.put(activeDeploymentContextEntry.getValue().getDeploymentId(), activeDeploymentContextEntry.getKey());
        }
    }

    /** This queue is used for internal events. */
    private List<AbstractMonitorEvent> internalProviderEventsQueue = Lists.newLinkedList();

    @Override
    public boolean receiveUnknownEvents() {
        return false;
    }

    @Override
    public String getAlienDeploymentId(Event event) {
        return paaSDeploymentIdToAlienDeploymentIdMapping.get(event.getContext().getDeploymentId());
    }

    /**
     * Get the orchestrator Id.
     * 
     * @return The orchestrator id.
     */
    public String getOrchestratorId() {
        return orchestratorPluginService.getOrchestratorId(orchestratorPlugin);
    }

    @SneakyThrows
    private void initLastAcknowledgedDate() {
        // We need to get the last event received by this orchestrator. Alien right now don't provide the orchestrator id to an orchestrator plugin.
        String orchestratorId = getOrchestratorId();
        // We override the get event since from alien4cloud here as we leverage an internal mechanisms for event queries that does not rely on the get event
        // since.
        Set<Class<?>> eventClasses = Sets.newHashSet();
        try {
            eventClasses = TypeScanner.scanTypes("alien4cloud.paas.model", AbstractMonitorEvent.class);
            // The PaaSDeploymentStatusMonitorEvent is an internal generated event and we should not take it into account.
            eventClasses.remove(PaaSDeploymentStatusMonitorEvent.class);
        } catch (ClassNotFoundException e) {
            log.info("No event class derived from {} found", AbstractMonitorEvent.class.getName());
        }
        Map<String, String[]> filter = Maps.newHashMap();
        filter.put("orchestratorId", new String[] { orchestratorId });

        // sort by filed date DESC
        QueryHelper.ISearchQueryBuilderHelper searchQueryHelperBuilder = alienMonitorDao.getQueryHelper().buildQuery()
                .types(eventClasses.toArray(new Class<?>[eventClasses.size()])).filters(filter).prepareSearch("deploymentmonitorevents")
                .fieldSort("date", true);

        // the first one is the one with the latest date
        GetMultipleDataResult lastestEventResult = alienMonitorDao.search(searchQueryHelperBuilder, 0, 10);
        if (lastestEventResult.getData().length > 0) {
            AbstractMonitorEvent lastEvent = (AbstractMonitorEvent) lastestEventResult.getData()[0];
            Date lastEventDate = new Date(lastEvent.getDate());
            log.info("Cfy Manager recovering events from the last in elasticsearch {} of type {}", lastEventDate, lastEvent.getClass().getName());
            this.lastPollingDate = lastEventDate;
        } else {
            log.debug("No monitor events found");
        }
    }

    @Override
    public Date lastAcknowledgedDate() {
        if (lastPollingDate == null) {
            initLastAcknowledgedDate();
        }
        return lastPollingDate;
    }

    @Override
    public synchronized void accept(CloudifyEvent[] cloudifyEvents) {
        List<CloudifyEvent> filteredEvents = Lists.newArrayList();
        for (CloudifyEvent event : cloudifyEvents) {
            if (event.getEvent().getEventType() == null) {
                continue;
            }
            if (EventType.TASK_SUCCEEDED.equals(event.getEvent().getEventType()) && CloudifyLifeCycle.START.equals(event.getEvent().getContext().getOperation())
                    || CloudifyLifeCycle.CONFIGURE.equals(event.getEvent().getContext().getOperation())
                    || CloudifyLifeCycle.CREATE.equals(event.getEvent().getContext().getOperation())
                    || CloudifyLifeCycle.DELETE.equals(event.getEvent().getContext().getOperation())
                    || CloudifyLifeCycle.STOP.equals(event.getEvent().getContext().getOperation())) {
                filteredEvents.add(event);
            } else if (!Workflow.CREATE_DEPLOYMENT_ENVIRONMENT.equals(event.getEvent().getContext().getWorkflowId())
                    && !Workflow.EXECUTE_OPERATION.equals(event.getEvent().getContext().getWorkflowId())
                    && !Workflow.UNINSTALL.equals(event.getEvent().getContext().getWorkflowId())
                    && (EventType.A4C_PERSISTENT_EVENT.equals(event.getEvent().getEventType())
                            || EventType.A4C_WORKFLOW_EVENT.equals(event.getEvent().getEventType())
                            || EventType.A4C_WORKFLOW_STARTED.equals(event.getEvent().getEventType()))

            ) {
                filteredEvents.add(event);
            }
        }

        List<AbstractMonitorEvent> alienEvents = toAlienEvents(filteredEvents);
        for (AbstractMonitorEvent event : alienEvents) {
            internalProviderEventsQueue.add(event);
            if (lastPollingDate == null || lastPollingDate.getTime() < event.getDate()) {
                lastPollingDate = new Date(event.getDate());
            }
        }
    }

    public synchronized ListenableFuture<AbstractMonitorEvent[]> getEventsSince(final Date lastTimestamp, int batchSize) {
        // Process internal events
        final ListenableFuture<AbstractMonitorEvent[]> internalEvents = processInternalQueue(batchSize);
        if (internalEvents != null) {
            // Deliver internal events first, next time when Alien poll, we'll deliver cloudify events
            return internalEvents;
        }
        // There is nothing to return.
        return Futures.immediateFuture(new AbstractMonitorEvent[0]);
    }

    public synchronized void registerDeployment(String deploymentPaaSId, String deploymentId) {
        paaSDeploymentIdToAlienDeploymentIdMapping.put(deploymentPaaSId, deploymentId);
        alienDeploymentIdToPaaSDeploymentIdMapping.put(deploymentId, deploymentPaaSId);
    }

    public synchronized void registerDeploymentEvent(String deploymentPaaSId, DeploymentStatus deploymentStatus) {
        if (paaSDeploymentIdToAlienDeploymentIdMapping.containsKey(deploymentPaaSId)) {
            PaaSDeploymentStatusMonitorEvent deploymentStatusMonitorEvent = new PaaSDeploymentStatusMonitorEvent();
            deploymentStatusMonitorEvent.setDeploymentStatus(deploymentStatus);
            deploymentStatusMonitorEvent.setDeploymentId(paaSDeploymentIdToAlienDeploymentIdMapping.get(deploymentPaaSId));
            internalProviderEventsQueue.add(deploymentStatusMonitorEvent);
        } else {
            log.warn("Notify new status {} for the deployment {} which is not registered by event service", deploymentStatus, deploymentPaaSId);
        }
    }

    /**
     * Register an event to be added to the queue to dispatch it to Alien 4 Cloud.
     *
     * @param event The event to be dispatched.
     */
    public synchronized void registerEvent(AbstractMonitorEvent event) {
        internalProviderEventsQueue.add(event);
    }

    private ListenableFuture<AbstractMonitorEvent[]> processInternalQueue(int batchSize) {
        if (internalProviderEventsQueue.isEmpty()) {
            return null;
        }
        List<AbstractMonitorEvent> toBeReturned = internalProviderEventsQueue;
        if (internalProviderEventsQueue.size() > batchSize) {
            // There are more than the required batch
            toBeReturned = internalProviderEventsQueue.subList(0, batchSize);
        }
        try {
            if (log.isDebugEnabled()) {
                for (AbstractMonitorEvent event : toBeReturned) {
                    log.debug("Send event {} to Alien", event);
                }
            }
            return Futures.immediateFuture(toBeReturned.toArray(new AbstractMonitorEvent[toBeReturned.size()]));
        } finally {
            if (toBeReturned == internalProviderEventsQueue) {
                // Less than required batch
                internalProviderEventsQueue.clear();
            } else {
                // More than required batch
                List<AbstractMonitorEvent> newQueue = Lists.newLinkedList();
                for (int i = batchSize; i < internalProviderEventsQueue.size(); i++) {
                    newQueue.add(internalProviderEventsQueue.get(i));
                }
                internalProviderEventsQueue.clear();
                internalProviderEventsQueue = newQueue;
            }
        }
    }

    private List<AbstractMonitorEvent> toAlienEvents(List<CloudifyEvent> cloudifyEvents) {
        final List<AbstractMonitorEvent> alienEvents = Lists.newArrayList();
        for (CloudifyEvent cloudifyEvent : cloudifyEvents) {
            AbstractMonitorEvent alienEvent = toAlienEvent(cloudifyEvent);
            if (alienEvent != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Received event {}", cloudifyEvent);
                }
                alienEvents.add(alienEvent);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Filtered event {}", cloudifyEvent);
                }
            }
        }
        return alienEvents;
    }

    private AbstractMonitorEvent toAlienEvent(CloudifyEvent cloudifyEvent) {
        AbstractMonitorEvent alienEvent;
        switch (cloudifyEvent.getEvent().getEventType()) {
        case EventType.TASK_SUCCEEDED:
            String newInstanceState = CloudifyLifeCycle.getSucceededInstanceState(cloudifyEvent.getEvent().getContext().getOperation());
            if (newInstanceState == null) {
                return null;
            }
            PaaSInstanceStateMonitorEvent instanceTaskStartedEvent = new PaaSInstanceStateMonitorEvent();
            instanceTaskStartedEvent.setInstanceId(cloudifyEvent.getEvent().getContext().getNodeId());
            instanceTaskStartedEvent.setNodeTemplateId(cloudifyEvent.getEvent().getContext().getNodeName());
            instanceTaskStartedEvent.setInstanceState(newInstanceState);
            instanceTaskStartedEvent.setInstanceStatus(NodeInstanceStatus.getInstanceStatusFromState(newInstanceState));
            alienEvent = instanceTaskStartedEvent;
            break;
        case EventType.A4C_PERSISTENT_EVENT:
            log.info("Received persistent event " + cloudifyEvent.getEvent().getId());
            String persistentCloudifyEvent = cloudifyEvent.getEvent().getMessage().getText();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
            try {
                EventAlienPersistent eventAlienPersistent = objectMapper.readValue(persistentCloudifyEvent, EventAlienPersistent.class);
                // query API
                // TODO make that Async
                NodeInstance instance = nodeInstanceClient.read(cloudifyEvent.getEvent().getContext().getNodeId());
                Map<String, Object> persistentProperties = new HashMap<String, Object>(eventAlienPersistent.getPersistentProperties().size());
                for (Map.Entry<String, String> entry : eventAlienPersistent.getPersistentProperties().entrySet()) {
                    if (!instance.getRuntimeProperties().containsKey(entry.getKey())) {
                        // This is a workaround to ignore events from existing volumes especially on aws.
                        // Cloudify don't have a 'zone' runtime properties when using existing volumes.
                        // As it is existing volumes, we already has the persistent properties in A4C.
                        // So we just ignore the event for conveniency
                        log.warn("Ignore event. Couldn't find the key <{}> in the runtime properties of node instance <{}>", entry.getKey(), instance.getId());
                        return null;
                    }
                    String attributeValue = (String) MapUtil.get(instance.getRuntimeProperties(), entry.getKey());
                    persistentProperties.put(entry.getValue(), attributeValue);
                }
                alienEvent = new PaaSInstancePersistentResourceMonitorEvent(cloudifyEvent.getEvent().getContext().getNodeName(),
                        cloudifyEvent.getEvent().getContext().getNodeId(), persistentProperties);
            } catch (Exception e) {
                log.warn("Problem processing persistent event " + cloudifyEvent.getEvent().getId(), e);
                return null;
            }
            break;
        case EventType.A4C_WORKFLOW_STARTED:
            String wfCloudifyEvent = cloudifyEvent.getEvent().getMessage().getText();
            objectMapper = new ObjectMapper();
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
            try {
                EventAlienWorkflowStarted eventAlienWorkflowStarted = objectMapper.readValue(wfCloudifyEvent, EventAlienWorkflowStarted.class);
                PaaSWorkflowMonitorEvent pwme = new PaaSWorkflowMonitorEvent();
                pwme.setExecutionId(cloudifyEvent.getEvent().getContext().getExecutionId());
                pwme.setWorkflowId(eventAlienWorkflowStarted.getWorkflowName());
                pwme.setSubworkflow(eventAlienWorkflowStarted.getSubworkflow());
                alienEvent = pwme;
            } catch (IOException e) {
                return null;
            }
            break;
        case EventType.A4C_WORKFLOW_EVENT:
            wfCloudifyEvent = cloudifyEvent.getEvent().getMessage().getText();
            objectMapper = new ObjectMapper();
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
            try {
                EventAlienWorkflow eventAlienPersistent = objectMapper.readValue(wfCloudifyEvent, EventAlienWorkflow.class);
                PaaSWorkflowStepMonitorEvent e = new PaaSWorkflowStepMonitorEvent();
                e.setNodeId(cloudifyEvent.getEvent().getContext().getNodeName());
                e.setInstanceId(cloudifyEvent.getEvent().getContext().getNodeId());
                e.setStepId(eventAlienPersistent.getStepId());
                e.setStage(eventAlienPersistent.getStage());
                String workflowId = cloudifyEvent.getEvent().getContext().getWorkflowId();
                e.setExecutionId(cloudifyEvent.getEvent().getContext().getExecutionId());
                if (workflowId.startsWith(Workflow.A4C_PREFIX)) {
                    workflowId = workflowId.substring(Workflow.A4C_PREFIX.length());
                }
                e.setWorkflowId(cloudifyEvent.getEvent().getContext().getWorkflowId());
                alienEvent = e;
            } catch (IOException e) {
                return null;
            }
            break;
        default:
            return null;
        }
        alienEvent.setDate(cloudifyEvent.getTimestamp().getTimeInMillis());
        alienEvent.setDeploymentId(cloudifyEvent.getAlienDeploymentId());
        return alienEvent;
    }
}
