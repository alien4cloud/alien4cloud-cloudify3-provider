package alien4cloud.paas.cloudify3.eventpolling;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Just a cache that stores event id for deduplication.
 * TODO: a TTL to avoid heap leak
 */
public class EventCache {

    private Set<String> ids = Sets.newHashSet();

    private Map<Long, Set<String>> horodatedEventMap = Maps.newHashMap();

    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public boolean add(EventReference e) {
        lock.writeLock().lock();
        try {
            boolean added = ids.add(e.id);
            if (added) {
                Set<String> horodatedEvents = horodatedEventMap.get(e.timestamp);
                if (horodatedEvents == null) {
                    horodatedEvents = Sets.newHashSet();
                    horodatedEventMap.put(e.timestamp, horodatedEvents);
                }
                horodatedEvents.add(e.id);
            }
            return added;
        } finally {
            lock.writeLock().unlock();
        }
    }

    // TODO: implements if needed all other methods
}
