package alien4cloud.paas.cloudify3.shared;

import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.util.DateUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Delayed poller instance is used to recover missed events from cfy as elasticsearch under load may delay some data indexing.
 */
@Slf4j
public class DelayedPollerInstance {
    private final String logPrefix;
    private final String managerUrl;
    private final ListeningScheduledExecutorService scheduler;
    private final EventClient logEventClient;
    private final PluginConfigurationHolder pluginConfigurationHolder;
    private final EventDispatcher eventDispatcher;
    private final boolean removeOlderEvents;

    private final AtomicBoolean stopPolling = new AtomicBoolean(false);
    private final AtomicBoolean panicMode = new AtomicBoolean(false);

    /** The polling delay. This poller will poll events with a delay on the current feed. */
    private final Long delay;

    /** Date of the last event recovered from the poller. */
    private Date fromDate = null;
    /** Date until which to poll events. We never poll events youger than this date as they may not be considered as stable enough. */
    private Date lastLiveEventDate = null;
    /** This is the local date on which the lastLiveEventDate has been set. */
    private long lastLiveEventSetDate = 0;
    /** The pagination start index. */
    private int from = 0;
    /** A lock to prevent settings from multithreaded access. */
    private final Lock resourceLock = new ReentrantLock();

    public DelayedPollerInstance(String managerUrl, ListeningScheduledExecutorService scheduler, EventClient logEventClient,
            PluginConfigurationHolder pluginConfigurationHolder, long delay, EventDispatcher eventDispatcher, boolean removeOlderEvents) {
        this.managerUrl = managerUrl;
        this.scheduler = scheduler;
        this.logEventClient = logEventClient;
        this.pluginConfigurationHolder = pluginConfigurationHolder;
        pluginConfigurationHolder.register(pluginConfiguration -> {swicthPanicMode();});
        this.delay = delay;
        this.eventDispatcher = eventDispatcher;
        this.removeOlderEvents = removeOlderEvents;
        logPrefix = "Delay " + delay + " feed";
        log.info("{}: Creating new delayed poller instance for manager <{}>", logPrefix, managerUrl);
        schedulePollLog();
    }

    private void swicthPanicMode() {
        log.warn("!!! Switch to panic mode !!!");
        panicMode.set(true);
    }

    public void initFromDate(Date fromDate) {
        resourceLock.lock();
        try {
            this.fromDate = fromDate;
        } finally {
            resourceLock.unlock();
        }
    }

    /**
     * Register the date of the last event polled by the live stream.
     *
     * @param lastLiveEventDate Date of the last event polled by the live stream.
     */
    public void registerLastLive(Date lastLiveEventDate) {
        resourceLock.lock();
        try {
            // Update the to date to keep the given delay.
            this.lastLiveEventDate = lastLiveEventDate;
            lastLiveEventSetDate = new Date().getTime();
            if (fromDate == null) {
                fromDate = lastLiveEventDate;
            }
        } finally {
            resourceLock.unlock();
        }
    }

    private void schedulePollLog() {
        resourceLock.lock();
        try {
            if (removeOlderEvents && fromDate != null) {
                eventDispatcher.getEventReceivedManager().remove(fromDate);
                eventDispatcher.getEventReceivedManager().logSize(log, logPrefix);
            }

            ListenableScheduledFuture<?> scheduledFuture;
            if ((lastLiveEventDate == null || fromDate.getTime() == lastLiveEventDate.getTime())) {
                scheduledFuture = scheduler.schedule(() -> {
                    log.debug("{}: No delay events to poll.", logPrefix);
                    schedulePollLog();
                }, delay / 2, TimeUnit.MILLISECONDS);
            } else {
                long scheduleTime = delay / 2 / 1000;
                log.debug("{}: Scheduling delay event poll request in {} seconds.", logPrefix, scheduleTime);
                scheduledFuture = scheduler.schedule(() -> triggerLogPolling(), scheduleTime, TimeUnit.SECONDS);
            }

            Futures.addCallback(scheduledFuture, new FutureCallback<Object>() {
                @Override
                public void onSuccess(Object o) {
                }

                @Override
                public void onFailure(Throwable throwable) {
                    log.error("{}: Error while polling for delayed events.", logPrefix, throwable);
                }
            });
        } finally {
            resourceLock.unlock();
        }
    }

    private void triggerLogPolling() {
        resourceLock.lock();
        try {
            // Compute the actual delay for toDate, we don't want to poll events that are too young so we keep a toDate that is the date of the last live event
            // minus delay time.
            // When no more events are received the delay will lower as we want to keep a delay based on the original system more than on the last event
            // received.
            long evolvedDelay = delay - (new Date().getTime() - lastLiveEventSetDate);
            if (evolvedDelay < 0) {
                evolvedDelay = 0;
            }
            Date toDate = new Date(lastLiveEventDate.getTime() - evolvedDelay);

            if (fromDate.compareTo(toDate) >= 0) {
                log.debug("{}: No more logs to poll.", logPrefix);
                schedulePollLog();
            }
            if (log.isDebugEnabled()) {
                log.debug("{}: Fetching logs with delay from date {} to date {}, from {}", logPrefix, DateUtil.logDate(fromDate), DateUtil.logDate(toDate), from);
            }
            // This may create a loop actually if there is batch size events in the given delay (30 secs).
            Futures.addCallback(logEventClient.asyncGetBatch(managerUrl, fromDate, toDate, from, EventServiceInstance.BATCH_SIZE), new EventCallBack());
        } finally {
            resourceLock.unlock();
        }
    }

    /**
     * The callback passed to the eventClient.
     */
    private class EventCallBack implements FutureCallback<Event[]> {
        @Override
        public void onSuccess(Event[] events) {
            if (panicMode.get()) {
                resourceLock.lock();
                try {
                    logStateAndEvents(events);
                    panicMode.set(false);
                } finally {
                    resourceLock.unlock();
                }
            }
            if (stopPolling.get()) {
                return;
            }
            log.debug("{}: Polled {} events", logPrefix, events.length);
            if (events.length == 0) {
                scheduler.submit(() -> { schedulePollLog(); });
                return;
            }

            // We will now eventually modify settings, so let's acquire a lock.
            resourceLock.lock();
            try {
                Date lastPolledEventDate = eventDispatcher.dispatch(fromDate, events, logPrefix);
                if (fromDate.equals(lastPolledEventDate)) {
                    if (log.isDebugEnabled()) {
                        log.debug("{}: from date: {} last event date: {}", logPrefix, DateUtil.logDate(fromDate), DateUtil.logDate(lastPolledEventDate));
                    }
                    // Events are still on the same date, let's poll for next batch
                    from += events.length;
                } else {
                    from = 0;
                    fromDate = lastPolledEventDate;
                }
            } finally {
                resourceLock.unlock();
            }

            // If the event batch is full then don't wait before polling again
            if (events.length == EventServiceInstance.BATCH_SIZE) {
                scheduler.submit(() -> { triggerLogPolling(); });
                return;
            }
            scheduler.submit(() -> { schedulePollLog(); });
        }

        @Override
        public void onFailure(Throwable throwable) {
            log.warn("{}: Unable to poll log event", logPrefix, throwable);
            if (!stopPolling.get()) {
                scheduler.submit(() -> { schedulePollLog(); });
            }
        }
    }

    /**
     * Just a backdoor to writeout state and events in case of polling failure.
     */
    private void logStateAndEvents(Event[] events) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

        log.warn("{} [Panic] from : {}", logPrefix, from);
        log.warn("{} [Panic] fromDate : {}", logPrefix, DateUtil.logDate(fromDate));
        log.warn("{} [Panic] lastLiveEventDate : {}", logPrefix, DateUtil.logDate(lastLiveEventDate));
        log.warn("{} [Panic] Event size : {}", logPrefix, events.length);

        int i = 0;
        for (Event event : events) {
            try {
                log.warn("{} [Panic] Event[{}] : {}", logPrefix, i, objectMapper.writeValueAsString(event));
            } catch (JsonProcessingException e) {
                log.warn("{} [Panic] Event[{}] : Not able to serialize event", logPrefix, i);            }
        }

    }

    public void preDestroy() {
        this.stopPolling.set(true);
        log.info("{}: Stopped delay log polling for manager {}", logPrefix, this.managerUrl);
    }
}