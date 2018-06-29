package alien4cloud.paas.cloudify3.shared;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

import alien4cloud.paas.cloudify3.shared.model.LogBatch;
import alien4cloud.paas.cloudify3.shared.restclient.A4cLogClient;
import alien4cloud.paas.cloudify3.util.LogParser;
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
        log.info("Registered event consumer {} ", consumerId);

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

    private synchronized void schedulePollLog() {
        log.debug("Scheduling event poll request in {} seconds.", pluginConfigurationHolder.getPluginConfiguration().getDelayBetweenLogPolling());
        long scheduleTime = pluginConfigurationHolder.getPluginConfiguration().getDelayBetweenLogPolling();
        scheduler.schedule(this::triggerLogPolling, scheduleTime, TimeUnit.SECONDS);
    }

    /**
     * Add more resilience, mark and ignore the corrupted logs instead of polling always the same batch of events
     */
    private void triggerCorruptedLogPolling() {
        if (log.isDebugEnabled()) {
            log.debug("Corrupted poller: start");
        }
        Object response;
        try {
            response = a4cLogClient.asyncGetCorruptedLog().get();
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Corrupted poller: network error occurred", e);
            }
            // Some network error, continue to poll
            if (!isStopPolling()) {
                schedulePollLog();
            }
            return;
        }

        LogBatch logBatch = null;
        try {
            logBatch = LogParser.parseLog(new JSONObject((Map) response));
            if (log.isDebugEnabled()) {
                log.debug("Corrupted poller: polled {} events", logBatch.getEntries().length);
            }

            // Convert the data and dispatch.
            if (!logBatch.getId().equals(lastAckId)) {
                eventDispatcher.dispatch(logBatch.getEntries(), "Live feed");
            } else if (log.isDebugEnabled()){
                log.debug("Already processed batch {}", logBatch.getId());
            }

        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.debug("Corrupted poller: some error occurred", e);
            }
        } finally {
            // Ack and trigger next batch
            ackAndTriggerNext(logBatch.getId());
        }
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
                if (log.isDebugEnabled()) {
                    log.debug("Some error occurred when polling the logs", t.getMessage());
                }
                if (!stopPolling) {
                    // Should continue when encountering some corrupted logs
                    triggerCorruptedLogPolling();
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