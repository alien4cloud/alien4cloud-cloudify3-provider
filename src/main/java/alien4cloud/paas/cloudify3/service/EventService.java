package alien4cloud.paas.cloudify3.service;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.xml.bind.DatatypeConverter;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import alien4cloud.paas.cloudify3.model.*;
import alien4cloud.paas.cloudify3.restclient.DeploymentEventClient;
import alien4cloud.paas.cloudify3.restclient.NodeInstanceClient;
import alien4cloud.paas.model.*;
import alien4cloud.utils.MapUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Handle cloudify 3 events request
 */
@Component("cloudify-event-service")
@Slf4j
public class EventService {

    @Resource
    private DeploymentEventClient eventClient;
    @Resource
    private NodeInstanceClient nodeInstanceClient;
    /**
     * Hold last event ids
     */
    private Set<String> lastEvents;
    private long lastRequestedTimestamp;

    // TODO : May manage in a better manner this kind of state
    private Map<String, String> paaSDeploymentIdToAlienDeploymentIdMapping = Maps.newConcurrentMap();

    private Map<String, String> alienDeploymentIdToPaaSDeploymentIdMapping = Maps.newConcurrentMap();

    public void init(Map<String, PaaSTopologyDeploymentContext> activeDeploymentContexts) {
        for (Map.Entry<String, PaaSTopologyDeploymentContext> activeDeploymentContextEntry : activeDeploymentContexts.entrySet()) {
            paaSDeploymentIdToAlienDeploymentIdMapping.put(activeDeploymentContextEntry.getKey(), activeDeploymentContextEntry.getValue().getDeploymentId());
            alienDeploymentIdToPaaSDeploymentIdMapping.put(activeDeploymentContextEntry.getValue().getDeploymentId(), activeDeploymentContextEntry.getKey());
        }
    }

    /**
     * This queue is used for internal events
     */
    private List<AbstractMonitorEvent> internalProviderEventsQueue = Lists.newLinkedList();

    private static final long delay = 30 * 1000L;

    public synchronized ListenableFuture<AbstractMonitorEvent[]> getEventsSince(final Date lastTimestamp, int batchSize) {
        // TODO Workaround as cloudify 3 seems do not respect appearance order of event based on timestamp
        Date requestTimestamp = new Date(lastTimestamp.getTime());
        if (lastEvents != null) {
            requestTimestamp.setTime(requestTimestamp.getTime() - delay);
        } else {
            lastEvents = Sets.newConcurrentHashSet();
        }
        // Process internal events
        final ListenableFuture<AbstractMonitorEvent[]> internalEvents = processInternalQueue(batchSize);
        if (internalEvents != null) {
            // Deliver internal events first, next time when Alien poll, we'll deliver cloudify events
            return internalEvents;
        }
        // Try to get events from cloudify
        ListenableFuture<Event[]> eventsFuture;
        // If the request is on the same timestamp then iterate from the last event size
        // TODO It's like a queue consumption and it's really ugly
        if (lastRequestedTimestamp == lastTimestamp.getTime()) {
            eventsFuture = eventClient.asyncGetBatch(null, requestTimestamp, lastEvents.size(), batchSize);
        } else {
            eventsFuture = eventClient.asyncGetBatch(null, requestTimestamp, 0, batchSize);
        }
        Function<Event[], AbstractMonitorEvent[]> cloudify3ToAlienEventsAdapter = new Function<Event[], AbstractMonitorEvent[]>() {
            @Override
            public AbstractMonitorEvent[] apply(Event[] cloudifyEvents) {
                // Convert cloudify events to alien events
                List<Event> eventsAfterFiltering = Lists.newArrayList();
                for (Event cloudifyEvent : cloudifyEvents) {
                    if (!lastEvents.contains(cloudifyEvent.getId())) {
                        eventsAfterFiltering.add(cloudifyEvent);
                    } else if (log.isDebugEnabled()) {
                        log.debug("Filtering event " + cloudifyEvent.getId() + ", last events size " + lastEvents.size());
                    }
                }
                if (lastRequestedTimestamp != lastTimestamp.getTime()) {
                    // Only clear last events if the last requested timestamp has changed
                    lastEvents.clear();
                }
                lastRequestedTimestamp = lastTimestamp.getTime();
                for (Event cloudifyEvent : cloudifyEvents) {
                    lastEvents.add(cloudifyEvent.getId());
                }
                List<AbstractMonitorEvent> alienEvents = toAlienEvents(eventsAfterFiltering);
                return alienEvents.toArray(new AbstractMonitorEvent[alienEvents.size()]);
            }
        };
        return Futures.transform(eventsFuture, cloudify3ToAlienEventsAdapter);
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
        return paaSDeploymentIdToAlienDeploymentIdMapping.get(deploymentPaaSId);
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

    private AbstractMonitorEvent toAlienEvent(Event cloudifyEvent) {
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
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
            try {
                EventAlienPersistent eventAlienPersistent = objectMapper.readValue(persistentCloudifyEvent, EventAlienPersistent.class);
                // query API
                // TODO make that Async
                NodeInstance instance = nodeInstanceClient.read(cloudifyEvent.getContext().getNodeId());
                String attributeValue = (String) MapUtil.get(instance.getRuntimeProperties(), eventAlienPersistent.getPersistentResourceId());
                alienEvent = new PaaSInstancePersistentResourceMonitorEvent(cloudifyEvent.getContext().getNodeName(), cloudifyEvent.getContext().getNodeId(),
                        eventAlienPersistent.getPersistentAlienAttribute(), attributeValue);

            } catch (Exception e) {
                log.warn("Problem processing persistent event " + cloudifyEvent.getId(), e);
                return null;
            }
            break;
        case EventType.A4C_WORKFLOW_STARTED:
            String wfCloudifyEvent = cloudifyEvent.getMessage().getText();
            objectMapper = new ObjectMapper();
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
            try {
                EventAlienWorkflowStarted eventAlienWorkflowStarted = objectMapper.readValue(wfCloudifyEvent, EventAlienWorkflowStarted.class);
                PaaSWorkflowMonitorEvent pwme = new PaaSWorkflowMonitorEvent();
                pwme.setExecutionId(cloudifyEvent.getContext().getExecutionId());
                pwme.setWorkflowId(eventAlienWorkflowStarted.getWorkflowName());
                pwme.setSubworkflow(eventAlienWorkflowStarted.getSubworkflow());
                alienEvent = pwme;
            } catch (IOException e) {
                return null;
            }
            break;
        case EventType.A4C_WORKFLOW_EVENT:
            wfCloudifyEvent = cloudifyEvent.getMessage().getText();
            objectMapper = new ObjectMapper();
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
            try {
                EventAlienWorkflow eventAlienPersistent = objectMapper.readValue(wfCloudifyEvent, EventAlienWorkflow.class);
                PaaSWorkflowStepMonitorEvent e = new PaaSWorkflowStepMonitorEvent();
                e.setNodeId(cloudifyEvent.getContext().getNodeName());
                e.setInstanceId(cloudifyEvent.getContext().getNodeId());
                e.setStepId(eventAlienPersistent.getStepId());
                e.setStage(eventAlienPersistent.getStage());
                String workflowId = cloudifyEvent.getContext().getWorkflowId();
                e.setExecutionId(cloudifyEvent.getContext().getExecutionId());
                if (workflowId.startsWith(Workflow.A4C_PREFIX)) {
                    workflowId = workflowId.substring(Workflow.A4C_PREFIX.length());
                }
                e.setWorkflowId(cloudifyEvent.getContext().getWorkflowId());
                alienEvent = e;
            } catch (IOException e) {
                return null;
            }
            break;
        default:
            return null;
        }
        alienEvent.setDate(DatatypeConverter.parseDateTime(cloudifyEvent.getTimestamp()).getTimeInMillis());
        String alienDeploymentId = paaSDeploymentIdToAlienDeploymentIdMapping.get(cloudifyEvent.getContext().getDeploymentId());
        if (alienDeploymentId == null) {
            if (log.isDebugEnabled()) {
                log.debug("Alien deployment id is not found for paaS deployment {}, must ignore this event {}", cloudifyEvent.getContext().getDeploymentId(),
                        cloudifyEvent);
            }
            return null;
        }
        alienEvent.setDeploymentId(alienDeploymentId);
        return alienEvent;
    }
}
