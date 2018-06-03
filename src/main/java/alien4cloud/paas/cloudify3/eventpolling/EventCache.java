package alien4cloud.paas.cloudify3.eventpolling;

import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.util.DateUtil;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import javax.annotation.PostConstruct;
import javax.xml.bind.DatatypeConverter;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Just a cache that stores event id for deduplication.
 */
@Slf4j
@Setter
public class EventCache {

    /**
     * Used to manage TTL.
     */
    private ScheduledExecutorService scheduler;

    /**
     * The duration we keep events in the cache.
     */
    private static final Duration TTL = Duration.ofMinutes(30);

    /**
     * The TTL will be checked each TTL_PERIOD min
     */
    private static final Duration TTL_PERIOD = Duration.ofMinutes(5);

    private final Set<String> ids = Sets.newHashSet();

    private final PriorityQueue<HorodatedEvents> queue = Queues.newPriorityQueue();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private static long _lastEventTimestamp;
    private static final Marker LAST_EVENT_DISPATCHED_MARKER = MarkerFactory.getMarker(EventCache.class.getName() + "_lastEventTimestamp");

    @PostConstruct
    public void init() {
        log.info("TTL will be checked each {} min removing events older than {} min", TTL_PERIOD.toMinutes(), TTL.toMinutes());
        scheduler.scheduleAtFixedRate(() -> manageTtl(), TTL_PERIOD.toMinutes(), TTL_PERIOD.toMinutes(), TimeUnit.MINUTES);
    }

    private void manageTtl() {
        lock.writeLock().lock();
        try {
            long startTime = Instant.now().toEpochMilli();
            if (log.isDebugEnabled(LAST_EVENT_DISPATCHED_MARKER)) {
                if (_lastEventTimestamp > 0) {
                    long ageInMillis = Instant.now().toEpochMilli() - _lastEventTimestamp;
                    String age = DurationFormatUtils.formatDuration(ageInMillis, "H:mm:ss");
                    log.debug("The last event referenced in the system was from {} it's age is {}", DateUtil.logDate(new Date(_lastEventTimestamp)), age);
                } else {
                    log.debug("No event has been yet referenced in the system.");
                }
            }
            // any event oldiest than this age will be removed;
            long threshold = Instant.now().minus(TTL).toEpochMilli();
            if (log.isDebugEnabled()) {
                log.debug("Manage TTL, all event oldiest than {} will be removed from cache", DateUtil.logDate(new Date(threshold)));
            }
            int removedEventCount = 0;
            HorodatedEvents leastElement = queue.peek();
            while(leastElement != null) {
                if (leastElement.timestamp < threshold) {
                    if (log.isTraceEnabled()) {
                        log.trace("Event #{} is removed from the cache since {} < {}", leastElement.id, DateUtil.logDate(new Date(leastElement.timestamp)), DateUtil.logDate(new Date(threshold)));
                    }
                    ids.remove(queue.poll().id);
                    removedEventCount++;
                    leastElement = queue.peek();
                } else {
                    // no event can be oldiest than the leastElement, we keep the remaining events.
                    break;
                }
            }
            if (log.isDebugEnabled()) {
                String duration = DurationFormatUtils.formatDuration(Instant.now().toEpochMilli() - startTime, "H:mm:ss.SSS");
                log.debug("{} events have been removed from the cache (TTL expired), cache size is now {} (took )", removedEventCount, duration);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Add the events to the cache if not exist.
     *
     * @param events
     * @return only events that have been really added.
     */
    public Event[] addAll(Collection<Event> events) {
        lock.writeLock().lock();
        try {
            Set<String> insertedEventIds = Sets.newHashSet();
            events.stream().filter(
                    event -> !ids.contains(event.getId())
            ).forEach(event -> {
                HorodatedEvents he = new HorodatedEvents(event);
                queue.add(he);
                ids.add(he.id);
                insertedEventIds.add(he.id);
                if (log.isDebugEnabled(LAST_EVENT_DISPATCHED_MARKER)) {
                    if (_lastEventTimestamp == 0 || he.timestamp > _lastEventTimestamp) {
                        _lastEventTimestamp = he.timestamp;
                    }
                }
            });
            List<Event> result = events.stream().filter(
                    event -> insertedEventIds.contains(event.getId())
            ).collect(Collectors.toList());
            if (log.isTraceEnabled() && result.size() > 0) {
                log.trace("{} events added to the cache, cache size is now {}", result.size(), ids.size());
            }
            return result.toArray(new Event[0]);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private class HorodatedEvents implements Comparable<HorodatedEvents> {
        private Long timestamp;
        private String id;

        public HorodatedEvents(Event event) {
            this.timestamp = DatatypeConverter.parseDateTime(event.getTimestamp()).getTimeInMillis();
            this.id = event.getId();
        }

        @Override
        public int compareTo(HorodatedEvents o) {
            return timestamp.compareTo(o.timestamp);
        }
    }

}