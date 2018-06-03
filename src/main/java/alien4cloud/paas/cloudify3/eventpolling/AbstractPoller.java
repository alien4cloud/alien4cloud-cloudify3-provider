package alien4cloud.paas.cloudify3.eventpolling;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.google.common.util.concurrent.ListenableFuture;

import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.shared.EventClient;
import alien4cloud.paas.cloudify3.shared.EventDispatcher;
import alien4cloud.paas.cloudify3.util.DateUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * A poller is responsible of polling all events of an epoch (a date interval) in batch mode.
 */
@Setter
@Getter
@Slf4j
public abstract class AbstractPoller {

    /**
     * Max size of the batch of events to retrieve for each REST request.
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

    protected void pollEpoch(Instant fromDate, Instant toDate)
            throws ExecutionException, InterruptedException {
        int offset = 0;
        while (true) {

            if (log.isDebugEnabled()) {
                log.debug("[{}] About to poll epoch between {} and {} offset {} with a batch size {}", getPollerNature(), DateUtil.logDate(fromDate), DateUtil.logDate(toDate), offset, BATCH_SIZE);
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
                if (log.isTraceEnabled()) {
                    log.trace("[{}] Event size reached batch size, increase offset to {} and continue polling", getPollerNature(), offset);
                }
            }
        }
    }
}
