package alien4cloud.paas.cloudify3.shared.model;

import alien4cloud.paas.cloudify3.model.Event;
import lombok.Getter;
import lombok.Setter;

/**
 * Enriched cloudify event with the addition of the alien4cloud deployment id associated to the event.
 */
@Getter
@Setter
public class CloudifyEvent {
    /** The alien4cloud deployment id matching the given event. */
    private String alienDeploymentId;
    /** The processed timestamp of the event. */
    private java.util.Calendar timestamp;
    /** The associated cloudify event. */
    private Event event;
}