package alien4cloud.paas.cloudify3.eventpolling;


import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.PriorityQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.google.common.collect.Queues;
import com.google.common.collect.Sets;

import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.util.DateUtil;
import alien4cloud.paas.cloudify3.util.SyspropConfig;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Just a cache that stores event id for deduplication.
 */
@Slf4j
@Setter
public class EventCache {

    /**
     * Url
     */
    private String url;

    /**
     * Used to manage TTL.
     */
    private ScheduledExecutorService scheduler;

    /**
     * The duration we keep events in the cache.
     */
    private static final Duration TTL = Duration.ofMinutes(SyspropConfig.getInt(SyspropConfig.EVENTCACHE_TTL_IN_MINUTES, 30));

    /**
     * The TTL will be checked each EVICTION_PERIOD min
     */
    private static final Duration EVICTION_PERIOD = Duration.ofMinutes(SyspropConfig.getInt(SyspropConfig.EVENTCACHE_EVICTION_PERIOD_IN_MINUTES, 5));

    /**
     * Ids.
     */
    private final Set<String> ids = Sets.newHashSet();

    /**
     * Priority queue.
     */
    private final PriorityQueue<HorodatedEvents> queue = Queues.newPriorityQueue();

    /**
     * Lock.
     */
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private static long _lastEventTimestamp;

    public void init() {
        logInfo("TTL will be checked each {} min removing events older than {} min", EVICTION_PERIOD.toMinutes(), TTL.toMinutes());
        scheduler.scheduleAtFixedRate(() -> {
            try {
                manageTtl();
            } catch (Exception e) {
                log.error("Fatal error occurred: ", e);
            }
        }, EVICTION_PERIOD.toMinutes(), EVICTION_PERIOD.toMinutes(), TimeUnit.MINUTES);
    }

    private void manageTtl() {
        lock.writeLock().lock();
        try {
            long startTime = Instant.now().toEpochMilli();
            if (_lastEventTimestamp > 0) {
                long ageInMillis = System.currentTimeMillis() - _lastEventTimestamp;
                String age = DurationFormatUtils.formatDuration(ageInMillis, "H:mm:ss");
                logDebug("The last event referenced in the system was from {} it's age is {}", DateUtil.logDate(new Date(_lastEventTimestamp)), age);
                if (ageInMillis > 1000 * 60 * 30) {
                    logWarn("Long inactivity or Cfy logstash status suspicion ? The last event referenced in the system was from {} it's age is {}, more than 30 min ...", DateUtil.logDate(new Date(_lastEventTimestamp)), age);
                }
            } else {
                logDebug("No event has been yet referenced in the system.");
            }
            // any event oldiest than this age will be removed;
            long threshold = Instant.now().minus(TTL).toEpochMilli();
            if (log.isDebugEnabled()) {
                logDebug("Manage TTL, all event oldiest than {} will be removed from cache", DateUtil.logDate(new Date(threshold)));
            }
            int removedEventCount = 0;
            HorodatedEvents leastElement = queue.peek();
            while(leastElement != null) {
                if (leastElement.timestamp < threshold) {
                    if (log.isTraceEnabled()) {
                        logTrace("Event #{} is removed from the cache since {} < {}", leastElement.id, DateUtil.logDate(new Date(leastElement.timestamp)), DateUtil.logDate(new Date(threshold)));
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
                logDebug("{} events have been removed from the cache (TTL expired), cache size is now {} (took {})", removedEventCount, ids.size(), duration);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Artificially mark this event as received so it will not be accepted anymore (even if it is re-polled).
     */
    public void blackList(String eventId) {
        lock.writeLock().lock();
        try {
            ids.add(eventId);
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
                if (he.timestamp > _lastEventTimestamp) {
                    _lastEventTimestamp = he.timestamp;
                }
            });
            List<Event> result = events.stream().filter(
                    event -> insertedEventIds.contains(event.getId())
            ).collect(Collectors.toList());
            if (log.isTraceEnabled() && result.size() > 0) {
                logTrace("{} events added to the cache, cache size is now {}", result.size(), ids.size());
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

    private void logTrace(String msg, Object... vars) {
        if (log.isTraceEnabled()) {
            log.trace("[@" + url +  "] " + msg, vars);
        }
    }

    private void logDebug(String msg, Object... vars) {
        if (log.isDebugEnabled()) {
            log.debug("[@" + url +  "] " + msg, vars);
        }
    }

    private void logInfo(String msg, Object... vars) {
        log.info("[@" + url +  "] " + msg, vars);
    }

    private void logWarn(String msg, Object... vars) {
        log.warn("[@" + url +  "] " + msg, vars);
    }
}
