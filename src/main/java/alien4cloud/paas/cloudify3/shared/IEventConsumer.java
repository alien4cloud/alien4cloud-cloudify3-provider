package alien4cloud.paas.cloudify3.shared;

import java.util.Date;
import java.util.function.Consumer;

import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.shared.model.CloudifyEvent;

/**
 * Interface to implement for a cloudify event consumer.
 */
public interface IEventConsumer extends Consumer<CloudifyEvent[]> {
    /**
     * Get the date of the last acknowledged event for this consumer.
     * 
     * @return The date of the last acknowledged event for the consumer.
     */
    Date lastAcknowledgedDate();

    /**
     * Get the alien deployment id of an event (if the consumer knows it).
     * 
     * @return The alien id based on a cloudify event.
     */
    String getAlienDeploymentId(Event event);

    /**
     * Flag to know if the consumer should receive events that he does not know.
     * 
     * @return True if the consumer should receive unknown events.
     */
    boolean receiveUnknownEvents();
}