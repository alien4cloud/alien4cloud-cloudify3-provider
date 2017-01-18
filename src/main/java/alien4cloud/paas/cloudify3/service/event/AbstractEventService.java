package alien4cloud.paas.cloudify3.service.event;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import alien4cloud.paas.cloudify3.model.Event;
import alien4cloud.paas.cloudify3.restclient.AbstractEventClient;
import lombok.extern.slf4j.Slf4j;

/**
 * Handle cloudify 3 events request, with offset when needed
 * This uses a local cache to store last received events
 */
@Slf4j
public abstract class AbstractEventService {

    /**
     * Hold last event ids
     */
    private Set<String> lastEvents = Sets.newConcurrentHashSet();
    private long lastRequestedTimestamp;

    protected int getOffSet(Date requestDate) {
        return requestDate != null && lastRequestedTimestamp == requestDate.getTime() ? lastEvents.size() : 0;
    }

    protected ListenableFuture<Event[]> getEvents(Date requestDate, int batchSize) {
        ListenableFuture<Event[]> future = getClient().asyncGetBatch(null, requestDate, getOffSet(requestDate), batchSize);

        // filter already received events adapter
        Function<Event[], Event[]> filterAlreadyReceived = events -> {
            List<Event> eventsAfterFiltering = Lists.newArrayList();
            for (Event cloudifyEvent : events) {
                if (!lastEvents.contains(cloudifyEvent.getId())) {
                    log.debug("Receiving event {}", cloudifyEvent.getId());
                    eventsAfterFiltering.add(cloudifyEvent);
                } else if (log.isDebugEnabled()) {
                    log.debug("Filtering event " + cloudifyEvent.getId() + ", last events size " + lastEvents.size());
                }
            }
            if (lastRequestedTimestamp != requestDate.getTime()) {
                // Only clear last events if the last requested timestamp has changed
                lastEvents.clear();
            }
            lastRequestedTimestamp = requestDate.getTime();
            for (Event cloudifyEvent : events) {
                lastEvents.add(cloudifyEvent.getId());
            }

            return eventsAfterFiltering.toArray(new Event[eventsAfterFiltering.size()]);
        };

        return Futures.transform(future, filterAlreadyReceived);
    }

    protected abstract AbstractEventClient getClient();

}
