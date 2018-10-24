package alien4cloud.paas.cloudify3.eventpolling;

import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.shared.restclient.EventClient;
import alien4cloud.paas.cloudify3.shared.EventDispatcher;
import alien4cloud.paas.cloudify3.util.DateUtil;
import com.google.common.util.concurrent.ListenableFuture;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * A poller is responsible of polling all events of an epoch (a date interval) in batch mode.
 * <p/>
 * The polling of an epoch is a blocking operation and must still a blocking operation.
 * If you change this, I hope for you I am not the man who will manage your production ;)
 */
@Getter
@Setter
public abstract class AbstractPoller {

    /**
     * Max size of the events batch to poll for each REST request.
     */
    protected static final int BATCH_SIZE = 100;

    /**
     * Retry delay
     */
    protected static final Duration RETRY_DELAY_IN_SECOND = Duration.ofSeconds(10);

    /**
     * The event client used to REST request cfy.
     */
    private EventClient eventClient;

    /**
     * The dispatcher.
     */
    @Resource(name="event-dispatcher")
    private EventDispatcher eventDispatcher;

    /**
     * The cache used to perform de-duplication.
     */
    @Resource
    private EventCache eventCache;

    /**
     * Url.
     */
    protected String url;

    /**
     * Poller should abort his work
     */
    private AtomicBoolean stopRequested = new AtomicBoolean(false);

    /**
     * @return a description of the poller.
     */
    public abstract String getPollerNature();

    /**
     * Start the poller.
     */
    public abstract void start();

    /**
     * @return the logger
     */
    protected abstract Logger getLogger();


    protected PollResult pollEpoch(Instant fromDate, Instant toDate) throws PollingException {
        return this.pollEpoch(fromDate, toDate, null);
    }

    /**
     * Poll the given epoch using the batch size, if necessary, iterate over offset to get the full event packet.
     * <p/>
     * This is a blocking operation. In a near future, we'll maybe make it multithreaded but this must be studied seriously.
     * <p/>
     * Having few blocking thread (4 with : live stream, 2 delayed stream and recovery stream) is not an issue when it permit to
     * preserve the health of the whole system.
     *
     * @param fromDate
     * @param toDate
     * @throws PollingException
     * @Returns the number of events dispatched
     */
    protected PollResult pollEpoch(Instant fromDate, Instant toDate, Function<List<Event>, List<Event>> eventFilter) throws PollingException {

        // this is the batch offset to poll
        int offset = 0;
        // the number of batches necessary to poll the whole epoch
        int batchCount = 0;
        // .....
        int retryCount = 0;

        long eventPolledCount = 0;
        long eventDispatchedCount = 0;

        // just a debug information, never use it in logic !
        Instant _startEpochPollingDate = Instant.now();
        if (getLogger().isDebugEnabled()) {
            logDebug("About to poll epoch {} -> {}", DateUtil.logDate(fromDate), DateUtil.logDate(toDate));
        }

        while (!stopRequested()) {
            logDebug("About to poll epoch {} -> {} offset: {}, batch size: {}", DateUtil.logDate(fromDate), DateUtil.logDate(toDate), offset, BATCH_SIZE);

            batchCount++;
            ListenableFuture<Event[]> future = getEventClient()
                    .asyncGetBatch(Date.from(fromDate), Date.from(toDate), offset, BATCH_SIZE);

            // Get the events
            List<Event> events = null;
            try {
                events = Arrays.asList(future.get());
                retryCount = 0;
            } catch (InterruptedException e) {
                if (stopRequested()) {
                    break;
                }
            } catch (Exception e) {
                // an exception occurred while polling epoch
//                if (retryCount >= MAX_RETRY_COUNT) {
//                    logWarn("An error occured while polling period ({}), have retried {} times, giving up", e.getMessage(), retryCount);
//                    String msg = String.format("[%s @%s] An error occurred while polling period %s -> %s, have already retried %d times", getPollerNature(), getUrl(), DateUtil.logDate(fromDate), DateUtil.logDate(toDate), retryCount);
//                    throw new PollingException(msg, e);
//                }
                try {
                    if (getLogger().isWarnEnabled()) {
                        logWarn("An error occured while polling period ({}) after {} retries, retrying in {}s", e.getMessage(), retryCount, RETRY_DELAY_IN_SECOND.getSeconds());
                    }
                    Thread.sleep(RETRY_DELAY_IN_SECOND.toMillis());
                    retryCount++;
                    // continue the loop, so continue to try request the same period until Cfy is up
                    continue;
                } catch (InterruptedException e1) {
                    if (stopRequested()) {
                        break;
                    }
                }
            }

            eventPolledCount += events.size();
            if (getLogger().isDebugEnabled()) {
                logDebug("{} polled events", events.size());
            }

            // No events have been received for this epoch batch, the epoch polling is finished
            if (events.isEmpty()) {
                logTrace("The last batch is empty, stop looping");
                break;
            }

            // deduplicate using cache
            Event[] newEvents = null;
            if (eventFilter != null) {
                // first filter then cache
                newEvents = getEventCache().addAll(eventFilter.apply(events));
            } else {
                newEvents = getEventCache().addAll(events);
            }
            eventDispatchedCount += newEvents.length;
            // newEvents are the events that are effectively been considered (not already polled)
            logTrace("After deduplication, {}/{} events will be dispatched", newEvents.length, events.size());
            // Dispatch the events
            getEventDispatcher().dispatch(newEvents, getPollerNature());

            // Increment the offset
            if (events.size() < BATCH_SIZE) {
                // Finish this epoch
                break;
            } else {
                offset += BATCH_SIZE;
                logTrace("Event size reached batch size, increase offset to {} and continue polling", offset);
            }
        }

        if (getLogger().isDebugEnabled()) {
            logDebug("End of epoch polling {} -> {}, {} events polled in {} batches, {} dispatched, took {} ms", DateUtil.logDate(fromDate), DateUtil.logDate(toDate), eventPolledCount, batchCount, eventDispatchedCount, _startEpochPollingDate.until(Instant.now(), ChronoUnit.MILLIS));
        }
        return new PollResult(eventPolledCount, eventDispatchedCount, batchCount);
    }

    protected boolean stopRequested() {
        return stopRequested.get();
    }

    protected void stop() {
        stopRequested.set(true);
    }

    protected final class PollResult {
        protected final long eventPolledCount;
        protected final long eventDispatchedCount;
        protected final long bactchCount;

        public PollResult(long eventPolledCount, long eventDispatchedCount, long bactchCount) {
            this.eventPolledCount = eventPolledCount;
            this.eventDispatchedCount = eventDispatchedCount;
            this.bactchCount = bactchCount;
        }
    }

    // FIXME : better implem with one method !!!
    protected void logTrace(String msg, Object... vars) {
        if (getLogger().isTraceEnabled()) {
            getLogger().trace("[" + getPollerNature() + " @" + getUrl() +  "] " + msg, vars);
        }
    }

    protected void logDebug(String msg, Object... vars) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("[" + getPollerNature() + " @" + getUrl() +  "] " + msg, vars);
        }
    }

    protected void logInfo(String msg, Object... vars) {
        getLogger().info("[" + getPollerNature() + " @" + getUrl() +  "] " + msg, vars);
    }

    protected void logWarn(String msg, Object... vars) {
        getLogger().warn("[" + getPollerNature() + " @" + getUrl() +  "] " + msg, vars);
    }

    protected void logError(String msg, Object... vars) {
        getLogger().error("[" + getPollerNature() + " @" + getUrl() +  "] " + msg, vars);
    }
}
