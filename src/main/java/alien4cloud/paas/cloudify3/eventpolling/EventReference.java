package alien4cloud.paas.cloudify3.eventpolling;

import lombok.AllArgsConstructor;

/**
 * Minimal stuff to ease caching and deduplication.
 */
@AllArgsConstructor
public class EventReference {

    public final String id;
    public final Long timestamp;

}
