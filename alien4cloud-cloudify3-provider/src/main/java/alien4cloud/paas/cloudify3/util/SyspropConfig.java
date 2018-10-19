package alien4cloud.paas.cloudify3.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SyspropConfig {

    /**
     * The number of threads used to schedule jobs (refresh status ...).
     */
    public static final String CLOUDIFY_SCHEDULER_CORE_SIZE = "a4c.cfy3.cloudify-scheduler.coreSize";

    /**
     * Config of the thread pool used to handle events REST responses (except for event polling).
     */
    public static final String CLOUDIFY_ASYNC_CORE_SIZE = "a4c.cfy3.cloudify-async-thread-pool.coreSize";
    public static final String CLOUDIFY_ASYNC_MAX_SIZE = "a4c.cfy3.cloudify-async-thread-pool.maxSize";
    public static final String CLOUDIFY_ASYNC_KEEPALIVESECONDS = "a4c.cfy3.cloudify-async-thread-pool.keepAliveSeconds";

    /**
     * The number of thread used to schedule events polling.
     */
    public static final String EVENT_SCHEDULER_CORE_SIZE = "a4c.cfy3.event-scheduler.coreSize";

    /**
     * Config of the thread pool used to handle events REST responses. Since we guarantee than no more
     * than a gran maximum of 4 threads will poll events at the same time in a synchronous way (recover, live, and 2 delayed)
     * no need to increase this value.
     */
    public static final String EVENT_ASYNC_CORE_SIZE = "a4c.cfy3.event-async-thread-pool.coreSize";
    public static final String EVENT_ASYNC_MAX_SIZE = "a4c.cfy3.event-async-thread-pool.maxSize";
    public static final String EVENT_ASYNC_KEEPALIVESECONDS = "a4c.cfy3.event-async-thread-pool.keepAliveSeconds";

    /**
     * The maximum time period between each LivePoller request to poll events. Determines the minimum frequency of the polls.
     * Ideally, if event frequency is not too high, an event request will be executed each POLL_PERIOD seconds.
     * Under heavy load, with a lot of events to poll, the thread will not sleep between each request, so the delay will be reduced
     * between events requests to reduce event receive delay.
     */
    public static final String LIVEPOLLER_POLL_PERIOD_IN_SECONDS = "a4c.cfy3.eventpolling.LivePoller.POLL_PERIOD_IN_SECONDS";

    /**
     * The interval that will be requested by the LivePoller, should be > POLL_PERIOD && < POLL_PERIOD * 2 to avoid event misses.
     */
    public static final String LIVEPOLLER_POLL_INTERVAL_IN_SECONDS = "a4c.cfy3.eventpolling.LivePoller.POLL_INTERVAL_IN_SECONDS";

    /**
     * At startup the recovery poller is responsible of query for event in the past (to get missed event when system was down).
     * It starts from the timestamp of the last event stored in A4C to the starting date of the system. If no event is found in
     * A4C, use this parameter to minus the current date.
     */
    public static final String RECOVERYPOLLER_MAX_HISTORY_PERIOD_IN_DAYS = "a4c.cfy3.eventpolling.RecoveryPoller.MAX_HISTORY_PERIOD_IN_DAYS";

    /**
     * When known, the recovery starts from the timestamp of the last event stored in A4C minus RECOVERY_INTERVAL_IN_SECONDS.
     * Should be at least >= LIVEPOLLER_POLL_INTERVAL_IN_SECONDS. If your system is under load and the DelayedPollers dispatch
     * a lot of events, you should set this to maximum delay of them to avoid event miss at startup.
     */
    public static final String RECOVERYPOLLER_RECOVERY_INTERVAL_IN_SECONDS = "a4c.cfy3.eventpolling.RecoveryPoller.RECOVERY_INTERVAL_IN_SECONDS";

    /**
     * You can deactivate the recovery polling at startup of the system by setting this to false or 0.
     */
    public static final String RECOVERYPOLLER_ACTIVATED = "a4c.cfy3.eventpolling.RecoveryPoller.ACTIVATED";

    /**
     * The event cache is here to prevent event duplication. This TTL is the time to leave for events in this cache.
     */
    public static final String EVENTCACHE_TTL_IN_MINUTES = "a4c.cfy3.eventpolling.EventCache.TTL_IN_MINUTES";

    /**
     * The frequency in which the expirated events (regarding TTL) will be check and eventually evicted from the event cache.
     */
    public static final String EVENTCACHE_EVICTION_PERIOD_IN_MINUTES = "a4c.cfy3.eventpolling.EventCache.EVICTION_PERIOD_IN_MINUTES";

    /**
     * To monitor it's deployment and executions, A4C query Cfy about executions status each {} seconds.
     */
    @Deprecated
    public static final String SNAPSHOT_TRIGGER_PERIOD_IN_SECONDS = "a4c.cfy3.service.SnapshotService.SNAPSHOT_TRIGGER_PERIOD_IN_SECONDS";

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
