package alien4cloud.paas.cloudify3.service.event;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.xml.bind.DatatypeConverter;

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
import alien4cloud.paas.cloudify3.configuration.CfyConnectionManager;
import alien4cloud.paas.cloudify3.model.CloudifyLifeCycle;
import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.model.EventAlienPersistent;
import alien4cloud.paas.cloudify3.model.EventAlienWorkflow;
import alien4cloud.paas.cloudify3.model.EventAlienWorkflowStarted;
import alien4cloud.paas.cloudify3.model.EventType;
import alien4cloud.paas.cloudify3.model.NodeInstance;
import alien4cloud.paas.cloudify3.model.NodeInstanceStatus;
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
 * Handle cloudify events request
 */
@Slf4j
@Component("cloudify-event-service")
public class EventService implements IEventConsumer {
    @Resource
    private CfyConnectionManager connectionManager;

    @Resource(name = "alien-monitor-es-dao")
    private IGenericSearchDAO alienMonitorDao;
    private Date lastPollingDate;

    private Map<String, String> paaSDeploymentIdToAlienDeploymentIdMapping = Maps.newConcurrentMap();
    private Map<String, String> alienDeploymentIdToPaaSDeploymentIdMapping = Maps.newConcurrentMap();

    public void init(Map<String, PaaSTopologyDeploymentContext> activeDeploymentContexts) {
        for (Map.Entry<String, PaaSTopologyDeploymentContext> activeDeploymentContextEntry : activeDeploymentContexts.entrySet()) {
            paaSDeploymentIdToAlienDeploymentIdMapping.put(activeDeploymentContextEntry.getKey(), activeDeploymentContextEntry.getValue().getDeploymentId());
        }
    }

    /** This queue is used for internal events. */
    private List<AbstractMonitorEvent> internalProviderEventsQueue = Lists.newLinkedList();

    @Override
    public boolean receiveUnknownEvents() {
        return false; // we receive only events that have a deployment id (see getAlienDeploymentId(Event event)).
    }

    @Override
    public String getAlienDeploymentId(Event event) {
        return paaSDeploymentIdToAlienDeploymentIdMapping.get(event.getContext().getDeploymentId());
    }

    @SneakyThrows
    private void initLastAcknowledgedDate() {
        // We need to get the last event received by this orchestrator. Alien right now don't provide the orchestrator id to an orchestrator plugin.
        String orchestratorId = connectionManager.getOrchestratorId();
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
            if (EventType.TASK_SUCCEEDED.equals(event.getEvent().getEventType()) || EventType.A4C_PERSISTENT_EVENT.equals(event.getEvent().getEventType())
                    || EventType.A4C_WORKFLOW_EVENT.equals(event.getEvent().getEventType())
                    || EventType.A4C_WORKFLOW_STARTED.equals(event.getEvent().getEventType())) {
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

    public synchronized String getDeploymentIdFromDeploymentPaaSId(String deploymentPaaSId) {
        return deploymentPaaSId == null ? null : paaSDeploymentIdToAlienDeploymentIdMapping.get(deploymentPaaSId);
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
                    log.debug("Accepted after processing event {}", cloudifyEvent);
                }
                alienEvents.add(alienEvent);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Filtered After processing event {}", cloudifyEvent.getEvent().getId());
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
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
            try {
                EventAlienPersistent eventAlienPersistent = objectMapper.readValue(persistentCloudifyEvent, EventAlienPersistent.class);
                // query API
                // TODO make that Async
                NodeInstance instance = connectionManager.getApiClient().getNodeInstanceClient().read(cloudifyEvent.getEvent().getContext().getNodeId());
                Map<String, Object> persistentProperties = new HashMap<String, Object>(eventAlienPersistent.getPersistentProperties().size());
                for (Map.Entry<String, String> entry : eventAlienPersistent.getPersistentProperties().entrySet()) {
                    if (!instance.getRuntimeProperties().containsKey(entry.getKey())) {
                        // This is a workaround to ignore events from existing volumes especially on aws.
                        // Cloudify don't have a 'zone' runtime properties when using existing volumes.
                        // As it is existing volumes, we already has the persistent properties in A4C.
                        // So we just ignore the event for convenience
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
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
            try {
                EventAlienWorkflowStarted eventAlienWorkflowStarted = objectMapper.readValue(wfCloudifyEvent, EventAlienWorkflowStarted.class);
                PaaSWorkflowMonitorEvent pwme = new PaaSWorkflowMonitorEvent();
                pwme.setExecutionId(cloudifyEvent.getEvent().getContext().getExecutionId());
                pwme.setWorkflowId(eventAlienWorkflowStarted.getWorkflowName());
                pwme.setSubworkflow(eventAlienWorkflowStarted.getSubworkflow());
                alienEvent = pwme;
            } catch (Exception e) {
                log.warn("Problem processing workflow started event " + cloudifyEvent.getEvent().getId(), e);
                return null;
            }
            break;
        case EventType.A4C_WORKFLOW_EVENT:
            wfCloudifyEvent = cloudifyEvent.getEvent().getMessage().getText();
            objectMapper = new ObjectMapper();
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
            try {
                EventAlienWorkflow eventAlienWorkflow = objectMapper.readValue(wfCloudifyEvent, EventAlienWorkflow.class);
                PaaSWorkflowStepMonitorEvent e = new PaaSWorkflowStepMonitorEvent();
                e.setNodeId(cloudifyEvent.getEvent().getContext().getNodeName());
                e.setInstanceId(cloudifyEvent.getEvent().getContext().getNodeId());
                e.setStepId(eventAlienWorkflow.getStepId());
                e.setStage(eventAlienWorkflow.getStage());
                e.setExecutionId(cloudifyEvent.getEvent().getContext().getExecutionId());
                e.setWorkflowId(cloudifyEvent.getEvent().getContext().getWorkflowId());
                alienEvent = e;
            } catch (Exception e) {
                log.warn("Problem processing workflow event " + cloudifyEvent.getEvent().getId(), e);
                return null;
            }
            break;
        default:
            return null;
        }
        alienEvent.setDate(DatatypeConverter.parseDateTime(cloudifyEvent.getEvent().getTimestamp()).getTimeInMillis());
        alienEvent.setDeploymentId(cloudifyEvent.getAlienDeploymentId());
        return alienEvent;
    }
}
