package alien4cloud.paas.cloudify3.shared;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.shared.model.CloudifyEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Event dispatcher is responsible for dispatching the right events to the right consumers.
 */
@Slf4j
public class EventDispatcher {
    /** Map of registered event services to this orchestrator. */
    private final Map<String, IEventConsumer> eventConsumers = Maps.newHashMap();

    public synchronized void register(String consumerId, IEventConsumer logEventConsumer) {
        this.eventConsumers.put(consumerId, logEventConsumer);
    }

    public synchronized Set<String> unRegister(String consumerId) {
        this.eventConsumers.remove(consumerId);
        return this.eventConsumers.keySet();
    }

    /**
     * Dispatch events to registered listeners.
     *
     * @param events The events as received from the cloudify API.
     * @return The last fetched date.
     */
    public synchronized void dispatch(Event[] events, String logPrefix) {
        Map<String, List<CloudifyEvent>> eventsPerConsumers = Maps.newHashMap();

        // Prepare batch of events per consumers
        for (Event event : events) {

            if (log.isTraceEnabled()) {
                log.trace("Dispatch event : {}, ", event.toString());
            }

            java.util.Calendar eventTimeStamp = DatatypeConverter.parseDateTime(event.getTimestamp());

            CloudifyEvent cloudifyEvent = new CloudifyEvent();
            cloudifyEvent.setEvent(event);
            cloudifyEvent.setTimestamp(eventTimeStamp);

            // find the deployment id of the event and register for consumers
            for (Entry<String, IEventConsumer> consumerEntry : this.eventConsumers.entrySet()) {
                String alienDeploymentId = consumerEntry.getValue().getAlienDeploymentId(event);
                addToDispatched(eventsPerConsumers, consumerEntry.getKey(), consumerEntry.getValue(), cloudifyEvent, alienDeploymentId);
            }
        }
        // Dispatch events to the targeted consumers
        for (Entry<String, List<CloudifyEvent>> eventsPerConsumer : eventsPerConsumers.entrySet()) {
            List<CloudifyEvent> consumerEvents = eventsPerConsumer.getValue();
            if (log.isDebugEnabled()) {
                log.debug("[{}]: Dispatching {} events to {}", logPrefix, consumerEvents.size(), eventsPerConsumer.getKey());
            }
            this.eventConsumers.get(eventsPerConsumer.getKey()).accept(consumerEvents.toArray(new CloudifyEvent[consumerEvents.size()]));
        }
    }

    private void addToDispatched(Map<String, List<CloudifyEvent>> eventsPerConsumers, String consumerKey, IEventConsumer consumer, CloudifyEvent cloudifyEvent,
            String alienDeploymentId) {
        if (alienDeploymentId == null && !consumer.receiveUnknownEvents()) {
            return; // Do not dispatch unknown events to consumers that don't support them.
        }

        if (alienDeploymentId != null) {
            cloudifyEvent.setAlienDeploymentId(alienDeploymentId);
        }

        List<CloudifyEvent> consumerEvents = eventsPerConsumers.get(consumerKey);
        if (consumerEvents == null) {
            consumerEvents = Lists.newArrayList();
            eventsPerConsumers.put(consumerKey, consumerEvents);
        }
        consumerEvents.add(cloudifyEvent);
    }
}
