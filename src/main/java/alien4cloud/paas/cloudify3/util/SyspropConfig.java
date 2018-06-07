package alien4cloud.paas.cloudify3.util;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by xdegenne on 05/06/2018.
 */
@Slf4j
public class SyspropConfig {

    /**
     * TODO: explain each config
     */
    public static final String CLOUDIFY_SCHEDULER_CORE_SIZE = "alien4cloud.paas.cloudify3.cloudify-scheduler.coreSize";
    public static final String CLOUDIFY_ASYNC_CORE_SIZE = "alien4cloud.paas.cloudify3.cloudify-async-thread-pool.coreSize";
    public static final String CLOUDIFY_ASYNC_MAX_SIZE = "alien4cloud.paas.cloudify3.cloudify-async-thread-pool.maxSize";
    public static final String CLOUDIFY_ASYNC_KEEPALIVESECONDS = "alien4cloud.paas.cloudify3.cloudify-async-thread-pool.keepAliveSeconds";


    public static final String EVENT_SCHEDULER_CORE_SIZE = "alien4cloud.paas.cloudify3.event-scheduler.coreSize";
    public static final String EVENT_ASYNC_CORE_SIZE = "alien4cloud.paas.cloudify3.event-async-thread-pool.coreSize";
    public static final String EVENT_ASYNC_MAX_SIZE = "alien4cloud.paas.cloudify3.event-async-thread-pool.maxSize";
    public static final String EVENT_ASYNC_KEEPALIVESECONDS = "alien4cloud.paas.cloudify3.event-async-thread-pool.keepAliveSeconds";
    public static final String LIVEPOLLER_POLL_PERIOD_IN_SECONDS = "alien4cloud.paas.cloudify3.eventpolling.LivePoller.POLL_PERIOD_IN_SECONDS";
    public static final String LIVEPOLLER_POLL_INTERVAL_IN_SECONDS = "alien4cloud.paas.cloudify3.eventpolling.LivePoller.POLL_INTERVAL_IN_SECONDS";

    /**
     * At startup the recovery poller is responsible of query for event in the past (to get missed event when system was down).
     * It starts from the timestamp of the last event stored in A4C to the starting date of the system. If no event is found in
     * A4C, use this parameter to minus the current date.
     */
    public static final String RECOVERYPOLLER_MAX_HISTORY_PERIOD_IN_DAYS = "alien4cloud.paas.cloudify3.eventpolling.RecoveryPoller.MAX_HISTORY_PERIOD_IN_DAYS";

    /**
     * You can deactivate the recovery polling at startup of the system by setting this to false or 0.
     */
    public static final String RECOVERYPOLLER_ACTIVATED = "alien4cloud.paas.cloudify3.eventpolling.RecoveryPoller.ACTIVATED";

    /**
     * The event cache is here to prevent event duplication. This TTL is the time to leave for events in this cache.
     */
    public static final String EVENTCACHE_TTL_IN_MINUTES = "alien4cloud.paas.cloudify3.eventpolling.EventCache.TTL_IN_MINUTES";

    /**
     * The frequency in which the expirated events (regarding TTL) will be check and eventually evicted from the event cache.
     */
    public static final String EVENTCACHE_EVICTION_PERIOD_IN_MINUTES = "alien4cloud.paas.cloudify3.eventpolling.EventCache.EVICTION_PERIOD_IN_MINUTES";

    /**
     * To monitor it's deployment and executions, A4C query Cfy about executions status each {} seconds.
     */
    public static final String SNAPSHOT_TRIGGER_PERIOD_IN_SECONDS = "alien4cloud.paas.cloudify3.service.SnapshotService.SNAPSHOT_TRIGGER_PERIOD_IN_SECONDS";

    public static long getLong(String key, long defaultValue) {
        String value = System.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        long usedValue = defaultValue;
        try {
            usedValue = Long.valueOf(value);
            log.warn("Using the system provided value for property {}={}", key, usedValue);
        } catch (Exception e) {
            log.warn("The value for property {}={} is not in the expected format, using default value ({})", key, value, usedValue);
        }
        return usedValue;
    }

    public static int getInt(String key, int defaultValue) {
        String value = System.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        int usedValue = defaultValue;
        try {
            usedValue = Integer.valueOf(value);
            log.warn("Using the system provided value for property {}={}", key, usedValue);
        } catch (Exception e) {
            log.warn("The value for property {}={} is not in the expected format, using default value ({})", key, value, usedValue);
        }
        return usedValue;
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = System.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        boolean usedValue = defaultValue;
        try {
            usedValue = Boolean.valueOf(value);
            log.warn("Using the system provided value for property {}={}", key, usedValue);
        } catch (Exception e) {
            log.warn("The value for property {}={} is not in the expected format, using default value ({})", key, value, usedValue);
        }
        return usedValue;
    }
}
