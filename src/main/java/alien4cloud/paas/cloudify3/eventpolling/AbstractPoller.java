package alien4cloud.paas.cloudify3.eventpolling;

import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.shared.EventClient;
import alien4cloud.paas.cloudify3.shared.EventDispatcher;
import alien4cloud.paas.cloudify3.util.DateUtil;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.xml.bind.DatatypeConverter;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

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

    private EventClient eventClient;

    private EventDispatcher eventDispatcher;

    private EventCache eventCache;

    public abstract void start();
    public abstract void shutdown();
    public abstract String getPollerNature();

    protected String url;

    protected void pollEpoch(Instant fromDate, Instant toDate)
            throws ExecutionException, InterruptedException {
        int offset = 0;
        while (true) {

            if (log.isDebugEnabled()) {
                log.debug("[{}] About to poll epoch beetwen {} and {} offset {} with a batch size {}", getPollerNature(), DateUtil.logDate(fromDate), DateUtil.logDate(toDate), offset, BATCH_SIZE);
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

            List<EventReference> refs = events.stream().map(e -> new EventReference(e.getId(),
                    DatatypeConverter.parseDateTime(e.getTimestamp()).getTimeInMillis())).collect(Collectors.toList());

            // The list of id added into cache
            Set<String> whiteList = refs.stream().map(r -> {
                if (getEventCache().add(r)) {
                    return r.id;
                } else {
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toSet());

            if (log.isTraceEnabled()) {
                log.trace("[{}] After deduplication, {}/{} events will be dispatched", getPollerNature(), whiteList.size(), events.size());
            }
            // Dispatch the events
            events.removeIf(e -> !whiteList.contains(e.getId()));
            getEventDispatcher().dispatch(events.toArray(new Event[0]), getPollerNature());

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
