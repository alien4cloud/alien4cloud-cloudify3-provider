package alien4cloud.paas.cloudify3.shared;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.bind.DatatypeConverter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.shared.model.CloudifyEvent;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Instance that polls events.
 */
@Slf4j
@Getter
public class EventServiceInstance {
    public static final int BATCH_SIZE = 100;
    private final EventClient logEventClient;
    private final ListeningScheduledExecutorService scheduler;
    private final PluginConfigurationHolder pluginConfigurationHolder;
    private final AtomicReference<ListenableScheduledFuture<?>> logPollingFuture = new AtomicReference<>();

    /** The cloudify manager url. */
    private final String managerUrl;
    /** Map of registered event services to this orchestrator. */
    private final Map<String, IEventConsumer> eventConsumers = Maps.newHashMap();

    private Date pollingFromDate = null;
    private int from = 0;
    // The requests uses a start date including which is also the date of the last consumed events (to not loose events that may have the same timestamp but not
    // indexed when our request was performed)
    // In order to not dispatch the same events twice we keep their ids.
    private Set<String> startIntervalEvents = Sets.newHashSet();

    /**
     * Create a new event service instance to fetch events.
     * 
     * @param logEventClient
     * @param scheduler
     * @param pluginConfigurationHolder
     */
    public EventServiceInstance(final String managerUrl, final EventClient logEventClient, final ListeningScheduledExecutorService scheduler,
            final PluginConfigurationHolder pluginConfigurationHolder) {
        this.managerUrl = managerUrl;
        this.logEventClient = logEventClient;
        this.scheduler = scheduler;
        this.pluginConfigurationHolder = pluginConfigurationHolder;

        pluginConfigurationHolder.register(pluginConfiguration -> {
            log.info("Update log polling configuration with a delay of {} seconds",
                    pluginConfigurationHolder.getPluginConfiguration().getDelayBetweenLogPolling());
            cancelRunningSchedule();
            if (pollingFromDate != null) {
                logPollingFuture.set(schedulePollLog());
            }
        });
    }

    /**
     * Register an event consumer.
     * 
     * @param consumerId The id of the consumer (should be the orchestrator id).
     * @param logEventConsumer The log event consumer that will received it's targeted logs.
     */
    public synchronized void register(String consumerId, IEventConsumer logEventConsumer) {
        this.eventConsumers.put(consumerId, logEventConsumer);

        Date lastAcknowledgedDate = logEventConsumer.lastAcknowledgedDate();
        log.info("Registed event consumer {} with date {} while last polling date was {}", consumerId, lastAcknowledgedDate, pollingFromDate);

        if (lastAcknowledgedDate == null) {
            return;
        }

        if (pollingFromDate == null || lastAcknowledgedDate.compareTo(pollingFromDate) < 0) {
            log.info("register events poller to fetch older events");
            pollingFromDate = lastAcknowledgedDate;
            if (pluginConfigurationHolder.getPluginConfiguration() != null) {
                // If the configuration is not set then we wait for it to be set before scheduling next poll.
                cancelRunningSchedule();
                logPollingFuture.set(schedulePollLog());
            }
        }
    }

    public synchronized Set<String> unRegister(String consumerId) {
        this.eventConsumers.remove(consumerId);
        return this.eventConsumers.keySet();
    }

    public void preDestroy() {
        cancelRunningSchedule();
    }

    private synchronized ListenableScheduledFuture<?> schedulePollLog() {
        log.debug("Scheduling event poll request in {} seconds.", pluginConfigurationHolder.getPluginConfiguration().getDelayBetweenLogPolling());
        long scheduleTime = pluginConfigurationHolder.getPluginConfiguration().getDelayBetweenLogPolling();
        return scheduler.schedule(() -> triggerLogPolling(), scheduleTime, TimeUnit.SECONDS);
    }

    private synchronized void triggerLogPolling() {
        log.debug("Triggering event poll request from date {} ", pollingFromDate.getTime());
        // This may create a loop actually if there is batch size events in the given delay (30 secs).
        Futures.addCallback(logEventClient.asyncGetBatch(managerUrl, pollingFromDate, from, from + BATCH_SIZE), new FutureCallback<Event[]>() {
            private boolean isCanceled() {
                ListenableScheduledFuture<?> currentScheduledFuture = logPollingFuture.get();
                if (currentScheduledFuture != null) {
                    synchronized (currentScheduledFuture) {
                        if (currentScheduledFuture.isCancelled()) {
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override
            public void onSuccess(Event[] events) {
                if (isCanceled()) {
                    return;
                }
                log.debug("Polled {} events", events.length);
                if (events.length == 0) {
                    logPollingFuture.set(schedulePollLog());
                    return;
                }
                Date lastPolledEventDate = dispatch(pollingFromDate, events);
                if (pollingFromDate.equals(lastPolledEventDate) && events.length > 0) {
                    // Events are still on the same date, let's poll for next batch
                    from = events.length;
                } else {
                    from = 0;
                    startIntervalEvents.clear();
                    pollingFromDate = lastPolledEventDate;
                }
                addLastPollingDateEvents(lastPolledEventDate, events);
                // If the event batch is full then don't wait before polling again
                if (events.length == BATCH_SIZE) {
                    triggerLogPolling();
                    return;
                }
                logPollingFuture.set(schedulePollLog());
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("Unable to poll log event", t);
                if (!isCanceled()) {
                    logPollingFuture.set(schedulePollLog());
                }
            }
        });
    }

    private void addLastPollingDateEvents(Date lastPolledEventDate, Event[] events) {
        for (Event event : events) {
            if (lastPolledEventDate.equals(DatatypeConverter.parseDateTime(event.getTimestamp()).getTime())) {
                startIntervalEvents.add(event.getId());
            }
        }
    }

    /**
     * Dispatch events to registered listeners.
     * 
     * @param events The events as received from the cloudify API.
     * @return The last fetched date.
     */
    private synchronized Date dispatch(final Date fromDate, Event[] events) {
        Map<String, List<CloudifyEvent>> eventsPerConsumers = Maps.newHashMap();
        Date lastDate = fromDate;

        // Prepare batch of events per consumers
        for (Event event : events) {
            // if event has already be consumed just ignore it

            CloudifyEvent cloudifyEvent = new CloudifyEvent();
            cloudifyEvent.setEvent(event);
            cloudifyEvent.setTimestamp(DatatypeConverter.parseDateTime(event.getTimestamp()));

            lastDate = cloudifyEvent.getTimestamp().getTime();

            // find the deployment id of the event and register for consumers
            for (Entry<String, IEventConsumer> consumerEntry : this.eventConsumers.entrySet()) {
                String alienDeploymentId = consumerEntry.getValue().getAlienDeploymentId(event);
                addToDispatched(eventsPerConsumers, consumerEntry.getKey(), consumerEntry.getValue(), cloudifyEvent, alienDeploymentId);
            }
        }
        // Dispatch events to the targeted consumers
        for (Entry<String, List<CloudifyEvent>> eventsPerConsumer : eventsPerConsumers.entrySet()) {
            this.eventConsumers.get(eventsPerConsumer.getKey())
                    .accept(eventsPerConsumer.getValue().toArray(new CloudifyEvent[eventsPerConsumer.getValue().size()]));
        }
        return lastDate;
    }

    private void addToDispatched(Map<String, List<CloudifyEvent>> eventsPerConsumers, String consumerKey, IEventConsumer consumer, CloudifyEvent cloudifyEvent,
            String alienDeploymentId) {
        if (alienDeploymentId == null && !consumer.receiveUnknownEvents()) {
            return; // Do not dispatch unknown events to consumers that don't support them.
        }

        if (alienDeploymentId != null) {
            cloudifyEvent.setAlienDeploymentId(alienDeploymentId);
        }

        if (consumer.lastAcknowledgedDate().getTime() > cloudifyEvent.getTimestamp().getTimeInMillis()) {
            log.debug("Consumer {} has already processed the event at time {} and will ignore it.", consumerKey,
                    cloudifyEvent.getTimestamp().getTimeInMillis());
            return; // The event has probably already be received by the consumer.
        }

        List<CloudifyEvent> consumerEvents = eventsPerConsumers.get(consumerKey);
        if (consumerEvents == null) {
            consumerEvents = Lists.newArrayList();
            eventsPerConsumers.put(consumerKey, consumerEvents);
        }
        consumerEvents.add(cloudifyEvent);
    }

    private void cancelRunningSchedule() {
        ListenableScheduledFuture<?> currentScheduledFuture = logPollingFuture.get();
        if (currentScheduledFuture != null) {
            synchronized (currentScheduledFuture) {
                // If there was one running then try to cancel it
                logPollingFuture.get().cancel(false);
            }
        }
    }
}
