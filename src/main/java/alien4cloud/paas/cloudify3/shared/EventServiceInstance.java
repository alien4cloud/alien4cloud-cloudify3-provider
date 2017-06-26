package alien4cloud.paas.cloudify3.shared;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.shared.restclient.EventClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Instance that polls events.
 */
@Slf4j
@Getter
public class EventServiceInstance {
    public static final int BATCH_SIZE = 100;
    private final EventClient eventClient;
    private final ListeningScheduledExecutorService scheduler;
    private final PluginConfigurationHolder pluginConfigurationHolder;

    private final EventReceivedManager eventReceivedManager = new EventReceivedManager();
    private final EventDispatcher eventDispatcher = new EventDispatcher(eventReceivedManager);
    private final List<DelayedPollerInstance> delayedPollerInstances;

    private boolean pollingFromNewDate = false;
    private Date nextPollingDate = null;
    private Date pollingFromDate = null;
    private int from = 0;

    private boolean stopPolling = false;
    private boolean initialized = false;

    /**
     * Create a new event service instance to fetch events.
     *
     * @param eventClient
     * @param scheduler
     * @param pluginConfigurationHolder
     */
    public EventServiceInstance(final EventClient eventClient, final ListeningScheduledExecutorService scheduler,
            final PluginConfigurationHolder pluginConfigurationHolder) {
        this.eventClient = eventClient;
        this.scheduler = scheduler;
        this.pluginConfigurationHolder = pluginConfigurationHolder;

        delayedPollerInstances = Lists.newArrayList(
                new DelayedPollerInstance(scheduler, eventClient, pluginConfigurationHolder, 30 * 1000, eventDispatcher, false),
                new DelayedPollerInstance(scheduler, eventClient, pluginConfigurationHolder, 5 * 60 * 1000, eventDispatcher, true));
    }

    /**
     * Register an event consumer.
     *
     * @param consumerId The id of the consumer (should be the orchestrator id).
     * @param logEventConsumer The log event consumer that will received it's targeted logs.
     */
    public synchronized void register(String consumerId, IEventConsumer logEventConsumer) {
        eventDispatcher.register(consumerId, logEventConsumer);

        Date lastAcknowledgedDate = logEventConsumer.lastAcknowledgedDate();

        log.info("Registed event consumer {} with date {} while last polling date was {}", consumerId, lastAcknowledgedDate, pollingFromDate);

        // There is no polling date currently registered.
        Date referenceDate = nextPollingDate == null ? pollingFromDate : nextPollingDate;
        if (referenceDate == null) {
            if (lastAcknowledgedDate == null) {
                nextPollingDate = new Date();
                pollingFromNewDate = true;
            } else {
                nextPollingDate = lastAcknowledgedDate;
            }
        } else if (lastAcknowledgedDate != null) {
            // If we are polling from a new date and not from the latest received event
            if (pollingFromNewDate) {
                if (lastAcknowledgedDate.compareTo(referenceDate) < 0) {
                    nextPollingDate = lastAcknowledgedDate;
                    pollingFromNewDate = false;
                } else {
                    // If we are not polling from a new date then we don't register dates in the past (as we already received the events).
                    return;
                }
            } else if (lastAcknowledgedDate.compareTo(referenceDate) > 0) {
                // If some earlier events have been received then fetch events from this date.
                nextPollingDate = lastAcknowledgedDate;
                pollingFromNewDate = false;
            }
        } else {
            return;
        }

        log.info("Register to fetch events from {}.", nextPollingDate);
        for (DelayedPollerInstance instance : delayedPollerInstances) {
            instance.initFromDate(nextPollingDate);
        }

        // If the configuration is not set then we wait for it to be set before scheduling next poll.
        initScheduling();
    }

    public synchronized Set<String> unRegister(String consumerId) {
        return eventDispatcher.unRegister(consumerId);
    }

    private synchronized void initScheduling() {
        if (initialized) {
            return;
        }
        log.info("Initialized log polling");
        schedulePollLog();
        initialized = true;
    }

    private synchronized ListenableScheduledFuture<?> schedulePollLog() {
        log.debug("Scheduling event poll request in {} seconds.", pluginConfigurationHolder.getPluginConfiguration().getDelayBetweenLogPolling());
        long scheduleTime = pluginConfigurationHolder.getPluginConfiguration().getDelayBetweenLogPolling();
        return scheduler.schedule(() -> triggerLogPolling(), scheduleTime, TimeUnit.SECONDS);
    }

    private synchronized void triggerLogPolling() {
        if (nextPollingDate != null) {
            pollingFromDate = nextPollingDate;
            for (DelayedPollerInstance instance : delayedPollerInstances) {
                instance.registerLastLive(pollingFromDate);
            }
            nextPollingDate = null;
        }

        log.debug("Triggering event poll request from date {} ", pollingFromDate.getTime());
        // This may create a loop actually if there is batch size events in the given delay (30 secs).
        Futures.addCallback(eventClient.asyncGetBatch(pollingFromDate, null, from, BATCH_SIZE), new FutureCallback<Event[]>() {
            @Override
            public void onSuccess(Event[] events) {
                if (stopPolling) {
                    return;
                }

                log.debug("Polled {} events", events.length);
                if (events.length == 0) {
                    schedulePollLog();
                    return;
                }

                Date lastPolledEventDate = eventDispatcher.dispatch(pollingFromDate, events, "Live feed");
                eventDispatcher.getEventReceivedManager().logSize(log, "Live feed");

                if (pollingFromDate.equals(lastPolledEventDate)) {
                    // Events are still on the same date, let's poll for next batch
                    from += events.length;
                } else {
                    from = 0;
                    pollingFromDate = lastPolledEventDate;
                    for (DelayedPollerInstance instance : delayedPollerInstances) {
                        instance.registerLastLive(pollingFromDate);
                    }
                }
                // If the event batch is full then don't wait before polling again
                if (events.length == BATCH_SIZE) {
                    triggerLogPolling();
                    return;
                }
                schedulePollLog();
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("Unable to poll log event", t);
                if (!stopPolling) {
                    schedulePollLog();
                }
            }
        });
    }

    public void preDestroy() {
        log.debug("Cancel running schedule.");
        stopPolling = true;
    }
}
