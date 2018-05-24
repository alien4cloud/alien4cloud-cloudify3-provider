package alien4cloud.paas.cloudify3.service.event;

import java.util.*;

import javax.annotation.Resource;
import javax.xml.bind.DatatypeConverter;

import alien4cloud.paas.cloudify3.model.*;
import alien4cloud.paas.model.*;
import org.elasticsearch.common.collect.Sets;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.paas.cloudify3.configuration.CfyConnectionManager;
import alien4cloud.paas.cloudify3.shared.IEventConsumer;
import alien4cloud.paas.cloudify3.shared.model.CloudifyEvent;
import alien4cloud.utils.MapUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

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

    private static Set<String> ACCEPTED_EVENTS = Sets.newHashSet();
    static {
        ACCEPTED_EVENTS.add(EventType.TASK_SUCCEEDED);
        ACCEPTED_EVENTS.add(EventType.TASK_FAILED);
        ACCEPTED_EVENTS.add(EventType.SENDING_TASK);
        ACCEPTED_EVENTS.add(EventType.A4C_PERSISTENT_EVENT);
        ACCEPTED_EVENTS.add(EventType.A4C_WORKFLOW_EVENT);
        ACCEPTED_EVENTS.add(EventType.A4C_WORKFLOW_RELATIONSHIP_STEP_EVENT);
        ACCEPTED_EVENTS.add(EventType.A4C_WORKFLOW_STARTED);
        ACCEPTED_EVENTS.add(EventType.WORKFLOW_STARTED);
        ACCEPTED_EVENTS.add(EventType.WORKFLOW_SUCCEEDED);
        ACCEPTED_EVENTS.add(EventType.WORKFLOW_FAILED);
    }

    @Override
    public synchronized void accept(CloudifyEvent[] cloudifyEvents) {
        List<CloudifyEvent> filteredEvents = Lists.newArrayList();
        for (CloudifyEvent event : cloudifyEvents) {
            if (event.getEvent().getEventType() == null) {
                continue;
            }
            if (log.isDebugEnabled()) {
                log.debug("Received an event of type {}", event.getEvent().getEventType());
            }
            if (ACCEPTED_EVENTS.contains(event.getEvent().getEventType())) {
                filteredEvents.add(event);
            }
        }

        List<AbstractMonitorEvent> alienEvents = toAlienEvents(filteredEvents);
        internalProviderEventsQueue.addAll(alienEvents);
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
            alienEvents.addAll(toAlienEvent(cloudifyEvent));
        }
        return alienEvents;
    }

    private List<AbstractMonitorEvent> toAlienEvent(CloudifyEvent cloudifyEvent) {
        List<AbstractMonitorEvent> alienEvents = new ArrayList<AbstractMonitorEvent>();
        switch (cloudifyEvent.getEvent().getEventType()) {
        case EventType.TASK_SUCCEEDED:

            TaskSucceededEvent taskSucceededEvent = new TaskSucceededEvent();
            taskSucceededEvent.setTaskId(cloudifyEvent.getEvent().getContext().getTaskId());
            taskSucceededEvent.setExecutionId(cloudifyEvent.getEvent().getContext().getExecutionId());
            alienEvents.add(taskSucceededEvent);

            String newInstanceState = CloudifyLifeCycle.getSucceededInstanceState(cloudifyEvent.getEvent().getContext().getOperation());
            if (newInstanceState != null) {
                PaaSInstanceStateMonitorEvent instanceTaskStartedEvent = new PaaSInstanceStateMonitorEvent();
                instanceTaskStartedEvent.setInstanceId(cloudifyEvent.getEvent().getContext().getNodeId());
                instanceTaskStartedEvent.setNodeTemplateId(cloudifyEvent.getEvent().getContext().getNodeName());
                instanceTaskStartedEvent.setInstanceState(newInstanceState);
                instanceTaskStartedEvent.setInstanceStatus(NodeInstanceStatus.getInstanceStatusFromState(newInstanceState));
                alienEvents.add(instanceTaskStartedEvent);
            }
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
                        return alienEvents;
                    }
                    String attributeValue = (String) MapUtil.get(instance.getRuntimeProperties(), entry.getKey());
                    persistentProperties.put(entry.getValue(), attributeValue);
                }
                PaaSInstancePersistentResourceMonitorEvent alienEvent = new PaaSInstancePersistentResourceMonitorEvent(cloudifyEvent.getEvent().getContext().getNodeName(),
                        cloudifyEvent.getEvent().getContext().getNodeId(), persistentProperties);
                alienEvents.add(alienEvent);
            } catch (Exception e) {
                log.warn("Problem processing persistent event " + cloudifyEvent.getEvent().getId(), e);
                return alienEvents;
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
                alienEvents.add(pwme);
            } catch (Exception e) {
                log.warn("Problem processing workflow started event " + cloudifyEvent.getEvent().getId(), e);
                return alienEvents;
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
                e.setOperationName(eventAlienWorkflow.getOperationName());
                alienEvents.add(e);

                if (StringUtils.hasText(eventAlienWorkflow.getOperationName())) {
                    AbstractWorkflowStepEvent wse = null;
                    switch(eventAlienWorkflow.getStage()) {
                        case EventAlienWorkflow.STAGE_IN:
                            wse = new WorkflowStepStartedEvent();
                            break;
                        case EventAlienWorkflow.STAGE_OK:
                            wse = new WorkflowStepCompletedEvent();
                            break;
                    }
                    if (wse != null) {
                        wse.setNodeId(cloudifyEvent.getEvent().getContext().getNodeName());
                        wse.setInstanceId(cloudifyEvent.getEvent().getContext().getNodeId());
                        wse.setStepId(eventAlienWorkflow.getStepId());
                        wse.setExecutionId(cloudifyEvent.getEvent().getContext().getExecutionId());
                        wse.setWorkflowId(cloudifyEvent.getEvent().getContext().getWorkflowId());
                        wse.setOperationName(eventAlienWorkflow.getOperationName());
                        alienEvents.add(wse);
                    }
                }
            } catch (Exception e) {
                log.warn("Problem processing workflow event " + cloudifyEvent.getEvent().getId(), e);
                return alienEvents;
            }
            break;
        case EventType.A4C_WORKFLOW_RELATIONSHIP_STEP_EVENT:
            wfCloudifyEvent = cloudifyEvent.getEvent().getMessage().getText();
            objectMapper = new ObjectMapper();
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
            try {
                EventAlienWorkflowRelationshipStepEvent eventAlienWorkflow = objectMapper.readValue(wfCloudifyEvent, EventAlienWorkflowRelationshipStepEvent.class);
                AbstractWorkflowStepEvent e = null;
                switch(eventAlienWorkflow.getStage()) {
                    case EventAlienWorkflow.STAGE_IN:
                        e = new WorkflowStepStartedEvent();
                        break;
                    case EventAlienWorkflow.STAGE_OK:
                        e = new WorkflowStepCompletedEvent();
                        break;
                }
                if (e != null) {
                    e.setNodeId(cloudifyEvent.getEvent().getContext().getNodeName());
                    e.setInstanceId(cloudifyEvent.getEvent().getContext().getNodeId());
                    e.setStepId(eventAlienWorkflow.getStepId());
                    e.setExecutionId(cloudifyEvent.getEvent().getContext().getExecutionId());
                    e.setWorkflowId(cloudifyEvent.getEvent().getContext().getWorkflowId());
                    e.setOperationName(eventAlienWorkflow.getOperationName());
                    e.setTargetNodeId(eventAlienWorkflow.getTargetNodeId());
                    e.setTargetInstanceId(eventAlienWorkflow.getTargetInstanceId());
                    alienEvents.add(e);
                }
            } catch (Exception e) {
                log.warn("Problem processing workflow event " + cloudifyEvent.getEvent().getId(), e);
                return alienEvents;
            }
            break;
        case EventType.WORKFLOW_STARTED:
            PaaSWorkflowStartedEvent wste = new PaaSWorkflowStartedEvent();
            String workflowId = cloudifyEvent.getEvent().getContext().getWorkflowId();
            wste.setWorkflowId(workflowId);
            if (workflowId.startsWith("a4c_")) {
                wste.setWorkflowName(workflowId.substring(4));
            }
            wste.setExecutionId(cloudifyEvent.getEvent().getContext().getExecutionId());
            alienEvents.add(wste);
            break;
        case EventType.WORKFLOW_SUCCEEDED:
            PaaSWorkflowSucceededEvent wse = new PaaSWorkflowSucceededEvent();
            wse.setWorkflowId(cloudifyEvent.getEvent().getContext().getWorkflowId());
            wse.setExecutionId(cloudifyEvent.getEvent().getContext().getExecutionId());
            alienEvents.add(wse);
            break;
        case EventType.WORKFLOW_FAILED:
            PaaSWorkflowFailedEvent wfe = new PaaSWorkflowFailedEvent();
            wfe.setWorkflowId(cloudifyEvent.getEvent().getContext().getWorkflowId());
            wfe.setExecutionId(cloudifyEvent.getEvent().getContext().getExecutionId());
            alienEvents.add(wfe);
            break;
        case EventType.SENDING_TASK:
            TaskSentEvent taskSentEvent = new TaskSentEvent();
            enrichTaskEvent(taskSentEvent, cloudifyEvent);
            alienEvents.add(taskSentEvent);
            break;
        case EventType.TASK_STARTED:
            TaskStartedEvent taskStartedEvent = new TaskStartedEvent();
            enrichTaskEvent(taskStartedEvent, cloudifyEvent);
            alienEvents.add(taskStartedEvent);
            break;
        case EventType.TASK_FAILED:
            TaskFailedEvent taskFailedEvent = new TaskFailedEvent();
            enrichTaskEvent(taskFailedEvent, cloudifyEvent);
            taskFailedEvent.setErrorCauses(cloudifyEvent.getEvent().getContext().getTaskErrorCauses());
            alienEvents.add(taskFailedEvent);
            break;
        }
        for (AbstractMonitorEvent e : alienEvents) {
            e.setDate(DatatypeConverter.parseDateTime(cloudifyEvent.getEvent().getTimestamp()).getTimeInMillis());
            e.setDeploymentId(cloudifyEvent.getAlienDeploymentId());
        }
        return alienEvents;
    }

    private void enrichTaskEvent(AbstractTaskEvent taskSentEvent, CloudifyEvent cloudifyEvent) {
        taskSentEvent.setTaskId(cloudifyEvent.getEvent().getContext().getTaskId());
        taskSentEvent.setExecutionId(cloudifyEvent.getEvent().getContext().getExecutionId());
        taskSentEvent.setInstanceId(cloudifyEvent.getEvent().getContext().getNodeId());
        taskSentEvent.setNodeId(cloudifyEvent.getEvent().getContext().getNodeName());
        if (StringUtils.hasText(cloudifyEvent.getEvent().getContext().getSourceId())) {
            // it's a relationship task, we use the source id as instanceId
            taskSentEvent.setInstanceId(cloudifyEvent.getEvent().getContext().getSourceId());
        }
        if (StringUtils.hasText(cloudifyEvent.getEvent().getContext().getSourceName())) {
            // it's a relationship task, we use the source name as nodeId
            taskSentEvent.setNodeId(cloudifyEvent.getEvent().getContext().getSourceName());
        }
        taskSentEvent.setTargetNodeId(cloudifyEvent.getEvent().getContext().getTargetName());
        taskSentEvent.setTargetInstanceId(cloudifyEvent.getEvent().getContext().getTargetId());
        taskSentEvent.setOperationName(cloudifyEvent.getEvent().getContext().getOperation());
    }

}
