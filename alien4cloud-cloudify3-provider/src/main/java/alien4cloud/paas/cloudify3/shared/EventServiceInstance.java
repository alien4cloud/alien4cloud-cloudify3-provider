package alien4cloud.paas.cloudify3.shared;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

import alien4cloud.paas.cloudify3.shared.model.LogBatch;
import alien4cloud.paas.cloudify3.shared.restclient.A4cLogClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Instance that polls events.
 */
@Slf4j
@Getter
public class EventServiceInstance {
    private final ListeningScheduledExecutorService scheduler;
    private final PluginConfigurationHolder pluginConfigurationHolder;

    private final A4cLogClient a4cLogClient;
    private final EventDispatcher eventDispatcher = new EventDispatcher();

    private boolean stopPolling = false;
    private boolean initialized = false;
    private Long lastAckId = null;

    /**
     * Create a new event service instance to fetch events.
     *
     * @param a4cLogClient The log server client.
     * @param scheduler The scheduler.
     * @param pluginConfigurationHolder
     */
    public EventServiceInstance(final A4cLogClient a4cLogClient, final ListeningScheduledExecutorService scheduler,
            final PluginConfigurationHolder pluginConfigurationHolder) {
        // Initialize the client to get logs from rest
        // Lookup for existing registration id
        this.scheduler = scheduler;
        this.pluginConfigurationHolder = pluginConfigurationHolder;
        this.a4cLogClient = a4cLogClient;
    }

    /**
     * Register an event consumer.
     *
     * @param consumerId The id of the consumer (should be the orchestrator id).
     * @param logEventConsumer The log event consumer that will received it's targeted logs.
     */
    public synchronized void register(String consumerId, IEventConsumer logEventConsumer) {
        eventDispatcher.register(consumerId, logEventConsumer);
        log.info("Registed event consumer {} with date {} while last polling date was {}", consumerId);

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
        log.debug("Triggering event poll request");
        // This may create a loop actually if there is batch size events in the given delay (30 secs).

        Futures.addCallback(a4cLogClient.asyncGet(), new FutureCallback<LogBatch>() {
            @Override
            public void onSuccess(LogBatch logBatch) {
                if (stopPolling) {
                    return;
                }
                int eventCount = logBatch.getEntries() == null ? 0 : logBatch.getEntries().length;
                log.debug("Polled {} events", eventCount);
                if (eventCount == 0) {
                    schedulePollLog();
                    return;
                }

                // Convert the data and dispatch.
                if (lastAckId != logBatch.getId()) {
                    eventDispatcher.dispatch(logBatch.getEntries(), "Live feed");
                } else {
                    log.info("Already processed batch {}", logBatch.getId());
                }

                // Ack and trigger next
                ackAndTriggerNext(logBatch.getId());
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

    private void ackAndTriggerNext(long batchId) {
        Futures.addCallback(a4cLogClient.asyncAck(batchId), new FutureCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                triggerLogPolling();
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("Unable to ack log request", t);
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