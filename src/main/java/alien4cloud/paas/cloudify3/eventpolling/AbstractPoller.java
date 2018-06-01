package alien4cloud.paas.cloudify3.eventpolling;

import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.shared.EventClient;
import alien4cloud.paas.cloudify3.shared.EventDispatcher;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.Setter;

import javax.xml.bind.DatatypeConverter;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * A poller is responsible of polling an epoch in batch mode.
 */
@Setter
@Getter
public abstract class AbstractPoller {

    /**
     * Max size of the batch of events to retrieve for each REST request.
     */
    protected static final int BATCH_SIZE = 100;

    private EventClient eventClient;

    private EventDispatcher eventDispatcher;
    private Set<EventReference> eventCache;

    public abstract void start();
    public abstract void shutdown();

    protected final String url;

    public AbstractPoller(String url) {
        this.url = url;
    }

    protected void pollEpoch(Instant fromDate, Instant toDate)
            throws ExecutionException, InterruptedException {
        int offset = 0;
        while (true) {
            ListenableFuture<Event[]> future = getEventClient()
                    .asyncGetBatch(url, Date.from(fromDate), Date.from(toDate), offset, AbstractPoller.BATCH_SIZE);
            // Get the events
            List<Event> events = Arrays.asList(future.get());

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

            // Dispatch the events
            events.removeIf(e -> !whiteList.contains(e.getId()));
            getEventDispatcher().dispatch(Date.from(fromDate), events.toArray(new Event[0]), "Live stream for <" + url + ">");

            // Increment the offset
            if (events.size() < BATCH_SIZE) {
                // Finish this epoch
                break;
            } else {
                offset += BATCH_SIZE;
            }
        }
    }
}
