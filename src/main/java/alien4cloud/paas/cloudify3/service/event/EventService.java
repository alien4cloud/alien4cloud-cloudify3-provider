package alien4cloud.paas.cloudify3.service.event;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.xml.bind.DatatypeConverter;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import alien4cloud.paas.cloudify3.model.CloudifyLifeCycle;
import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.model.EventAlienPersistent;
import alien4cloud.paas.cloudify3.model.EventAlienWorkflow;
import alien4cloud.paas.cloudify3.model.EventAlienWorkflowStarted;
import alien4cloud.paas.cloudify3.model.EventType;
import alien4cloud.paas.cloudify3.model.NodeInstance;
import alien4cloud.paas.cloudify3.model.NodeInstanceStatus;
import alien4cloud.paas.cloudify3.restclient.AbstractEventClient;
import alien4cloud.paas.cloudify3.restclient.DeploymentEventClient;
import alien4cloud.paas.cloudify3.restclient.NodeInstanceClient;
import alien4cloud.paas.model.AbstractMonitorEvent;
import alien4cloud.paas.model.DeploymentStatus;
import alien4cloud.paas.model.PaaSDeploymentStatusMonitorEvent;
import alien4cloud.paas.model.PaaSInstancePersistentResourceMonitorEvent;
import alien4cloud.paas.model.PaaSInstanceStateMonitorEvent;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import alien4cloud.paas.model.PaaSWorkflowMonitorEvent;
import alien4cloud.paas.model.PaaSWorkflowStepMonitorEvent;
import alien4cloud.utils.MapUtil;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Handle cloudify 3 events request
 */
@Component("cloudify-event-service")
@Slf4j
public class EventService extends AbstractEventService {

    @Resource
    @Setter
    private DeploymentEventClient eventClient;
    @Resource
    @Setter
    private NodeInstanceClient nodeInstanceClient;

    // TODO : May manage in a better manner this kind of state
    private Map<String, String> paaSDeploymentIdToAlienDeploymentIdMapping = Maps.newConcurrentMap();

    public void init(Map<String, PaaSTopologyDeploymentContext> activeDeploymentContexts) {
        for (Map.Entry<String, PaaSTopologyDeploymentContext> activeDeploymentContextEntry : activeDeploymentContexts.entrySet()) {
            paaSDeploymentIdToAlienDeploymentIdMapping.put(activeDeploymentContextEntry.getKey(), activeDeploymentContextEntry.getValue().getDeploymentId());
        }
    }

    /** This queue is used for internal events. */
    private List<AbstractMonitorEvent> internalProviderEventsQueue = Lists.newLinkedList();

    private static final long delay = 30 * 1000L;

    public synchronized ListenableFuture<AbstractMonitorEvent[]> getEventsSince(final Date lastTimestamp, int batchSize) {
        // TODO Workaround as cloudify 3 seems do not respect appearance order of event based on timestamp
        // Process internal events
        final ListenableFuture<AbstractMonitorEvent[]> internalEvents = processInternalQueue(batchSize);
        if (internalEvents != null) {
            // Deliver internal events first, next time when Alien poll, we'll deliver cloudify events
            return internalEvents;
        }
        // Try to get events from cloudify
        ListenableFuture<Event[]> eventsFuture = getEvents(lastTimestamp, batchSize);

        Function<Event[], AbstractMonitorEvent[]> cloudify3ToAlienEventsAdapter = new Function<Event[], AbstractMonitorEvent[]>() {
            @Override
            public AbstractMonitorEvent[] apply(Event[] cloudifyEvents) {
                log.debug("Polled events size: {}", cloudifyEvents.length);
                // Convert cloudify events to alien events
                List<AbstractMonitorEvent> alienEvents = toAlienEvents(Lists.newArrayList(cloudifyEvents));
                return alienEvents.toArray(new AbstractMonitorEvent[alienEvents.size()]);
            }
        };
        return Futures.transform(eventsFuture, cloudify3ToAlienEventsAdapter);
    }

    public synchronized void registerDeployment(String deploymentPaaSId, String deploymentId) {
        paaSDeploymentIdToAlienDeploymentIdMapping.put(deploymentPaaSId, deploymentId);
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

    private List<AbstractMonitorEvent> toAlienEvents(List<Event> cloudifyEvents) {
        final List<AbstractMonitorEvent> alienEvents = Lists.newArrayList();
        for (Event cloudifyEvent : cloudifyEvents) {
            AbstractMonitorEvent alienEvent = toAlienEvent(cloudifyEvent);
            if (alienEvent != null) {
                if (log.isDebugEnabled()) {
                    log.debug("Accepted after processing event {}", cloudifyEvent);
                }
                alienEvents.add(alienEvent);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Filtered After processing event {}", cloudifyEvent.getId());
                }
            }
        }
        return alienEvents;
    }

    private AbstractMonitorEvent toAlienEvent(Event cloudifyEvent) {

        String alienDeploymentId = paaSDeploymentIdToAlienDeploymentIdMapping.get(cloudifyEvent.getContext().getDeploymentId());
        if (alienDeploymentId == null) {
            if (log.isWarnEnabled()) {
                log.warn("Alien deployment id is not found for paaS deployment {}, must ignore this event {}", cloudifyEvent.getContext().getDeploymentId(),
                        cloudifyEvent.getId());
            }
            return null;
        }

        AbstractMonitorEvent alienEvent;
        switch (cloudifyEvent.getEventType()) {
        case EventType.TASK_SUCCEEDED:
            String newInstanceState = CloudifyLifeCycle.getSucceededInstanceState(cloudifyEvent.getContext().getOperation());
            if (newInstanceState == null) {
                return null;
            }
            PaaSInstanceStateMonitorEvent instanceTaskStartedEvent = new PaaSInstanceStateMonitorEvent();
            instanceTaskStartedEvent.setInstanceId(cloudifyEvent.getContext().getNodeId());
            instanceTaskStartedEvent.setNodeTemplateId(cloudifyEvent.getContext().getNodeName());
            instanceTaskStartedEvent.setInstanceState(newInstanceState);
            instanceTaskStartedEvent.setInstanceStatus(NodeInstanceStatus.getInstanceStatusFromState(newInstanceState));
            alienEvent = instanceTaskStartedEvent;
            break;
        case EventType.A4C_PERSISTENT_EVENT:
            log.info("Received persistent event " + cloudifyEvent.getId());
            String persistentCloudifyEvent = cloudifyEvent.getMessage().getText();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
            try {
                EventAlienPersistent eventAlienPersistent = objectMapper.readValue(persistentCloudifyEvent, EventAlienPersistent.class);
                // query API
                // TODO make that Async
                NodeInstance instance = nodeInstanceClient.read(cloudifyEvent.getContext().getNodeId());
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
                alienEvent = new PaaSInstancePersistentResourceMonitorEvent(cloudifyEvent.getContext().getNodeName(), cloudifyEvent.getContext().getNodeId(),
                        persistentProperties);
            } catch (Exception e) {
                log.warn("Problem processing persistent event " + cloudifyEvent.getId(), e);
                return null;
            }
            break;
        case EventType.A4C_WORKFLOW_STARTED:
            String wfCloudifyEvent = cloudifyEvent.getMessage().getText();
            objectMapper = new ObjectMapper();
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
            try {
                EventAlienWorkflowStarted eventAlienWorkflowStarted = objectMapper.readValue(wfCloudifyEvent, EventAlienWorkflowStarted.class);
                PaaSWorkflowMonitorEvent pwme = new PaaSWorkflowMonitorEvent();
                pwme.setExecutionId(cloudifyEvent.getContext().getExecutionId());
                pwme.setWorkflowId(eventAlienWorkflowStarted.getWorkflowName());
                pwme.setSubworkflow(eventAlienWorkflowStarted.getSubworkflow());
                alienEvent = pwme;
            } catch (Exception e) {
                log.warn("Problem processing workflow started event " + cloudifyEvent.getId(), e);
                return null;
            }
            break;
        case EventType.A4C_WORKFLOW_EVENT:
            wfCloudifyEvent = cloudifyEvent.getMessage().getText();
            objectMapper = new ObjectMapper();
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
            try {
                EventAlienWorkflow eventAlienWorkflow = objectMapper.readValue(wfCloudifyEvent, EventAlienWorkflow.class);
                PaaSWorkflowStepMonitorEvent e = new PaaSWorkflowStepMonitorEvent();
                e.setNodeId(cloudifyEvent.getContext().getNodeName());
                e.setInstanceId(cloudifyEvent.getContext().getNodeId());
                e.setStepId(eventAlienWorkflow.getStepId());
                e.setStage(eventAlienWorkflow.getStage());
                e.setExecutionId(cloudifyEvent.getContext().getExecutionId());
                e.setWorkflowId(cloudifyEvent.getContext().getWorkflowId());
                alienEvent = e;
            } catch (Exception e) {
                log.warn("Problem processing workflow event " + cloudifyEvent.getId(), e);
                return null;
            }
            break;
        default:
            return null;
        }
        alienEvent.setDate(DatatypeConverter.parseDateTime(cloudifyEvent.getTimestamp()).getTimeInMillis());
        alienEvent.setDeploymentId(alienDeploymentId);
        return alienEvent;
    }

    @Override
    protected AbstractEventClient getClient() {
        return eventClient;
    }
}
