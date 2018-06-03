package alien4cloud.paas.cloudify3.eventpolling;

import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.shared.EventClient;
import alien4cloud.paas.cloudify3.shared.EventDispatcher;
import alien4cloud.paas.cloudify3.util.DateUtil;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * A poller is responsible of polling all events of an epoch (a date interval) in batch mode.
 * <p/>
 * The polling of an epoch is a blocking operation and must still a blocking operation.
 * If you change this, I hope for you I am not the man who will manage your production ;)
 */
@Setter
@Getter
@Slf4j
public abstract class AbstractPoller {

    /**
     * Max size of the events batch to poll for each REST request.
     */
    protected static final int BATCH_SIZE = 100;

    /**
     * The event client used to REST request cfy.
     */
    private EventClient eventClient;

    private EventDispatcher eventDispatcher;

    /**
     * The cache used to perform de-duplication.
     */
    private EventCache eventCache;

    public abstract void start();
    public abstract void shutdown();

    /**
     * @return a description of the poller.
     */
    public abstract String getPollerNature();

    protected String url;

    /**
     * Poll the given epoch using the batch size, if necessary, iterate over offset to get the full event packet.
     * <p/>
     * This is a blocking operation. In a near future, we'll maybe make it multithreaded but this must be studied seriously.
     * <p/>
     * Having few blocking thread (4 with : live stream, 2 delayed stream and historical stream) is not an issue when it permit to
     * preserve the health of the whole system.
     *
     * @param fromDate
     * @param toDate
     * @throws ExecutionException
     * @throws InterruptedException
     */
    protected void pollEpoch(Instant fromDate, Instant toDate)
            throws ExecutionException, InterruptedException {

        // this is the batch offset to poll
        int offset = 0;
        // the number of batches necessary to poll the whole epoch
        int _batchCount = 0;

        // just a debug information, never use it in logic !
        Instant _startEpochPollingDate = null;
        if (log.isDebugEnabled()) {
            _startEpochPollingDate = Instant.now();
            log.debug("[{}] About to poll epoch {} -> {}", getPollerNature(), DateUtil.logDate(fromDate), DateUtil.logDate(toDate));
        }

        while (true) {

            if (log.isDebugEnabled()) {
                log.debug("[{}] About to poll epoch {} -> {} offset: {}, batch size: {}", getPollerNature(), DateUtil.logDate(fromDate), DateUtil.logDate(toDate), offset, BATCH_SIZE);
            }

            ListenableFuture<Event[]> future = getEventClient()
                    .asyncGetBatch(url, Date.from(fromDate), Date.from(toDate), offset, BATCH_SIZE);
            // Get the events
            List<Event> events = Arrays.asList(future.get());

            if (log.isDebugEnabled()) {
                log.debug("[{}] {} polled events", getPollerNature(), events.size());
            }

            // No events have been received for this epoch batch, the epoch polling is finished
            if (events.isEmpty()) {
                break;
            }

            // deduplicate using cache
            Event[] newEvents = getEventCache().addAll(events);
            // newEvents are the events that are effectively been considered (not already polled)

            if (log.isTraceEnabled()) {
                log.trace("[{}] After deduplication, {}/{} events will be dispatched", getPollerNature(), newEvents.length, events.size());
            }
            // Dispatch the events
            getEventDispatcher().dispatch(newEvents, getPollerNature());

            // Increment the offset
            if (events.size() < BATCH_SIZE) {
                // Finish this epoch
                break;
            } else {
                offset += BATCH_SIZE;
                _batchCount++;
                if (log.isTraceEnabled()) {
                    log.trace("[{}] Event size reached batch size, increase offset to {} and continue polling", getPollerNature(), offset);
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("[{}] End of epoch polling {} -> {}, {} batches, took {} ms", getPollerNature(), DateUtil.logDate(fromDate), DateUtil.logDate(toDate), _batchCount, _startEpochPollingDate.until(Instant.now(), ChronoUnit.MILLIS));
        }

    }
}
