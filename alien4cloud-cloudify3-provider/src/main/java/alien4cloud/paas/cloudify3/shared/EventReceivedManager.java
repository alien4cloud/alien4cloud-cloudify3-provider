package alien4cloud.paas.cloudify3.shared;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.AllArgsConstructor;

/**
 * Manages a list of received events to avoid duplicated events when looking for missing ones.
 */
public class EventReceivedManager {
    private List<EventReceived> receivedEventList = Lists.newLinkedList();
    private Set<String> receivedEventIds = Sets.newHashSet();

    /**
     * Add an event in the list of received events.
     */
    public synchronized void addEvent(String id, Date date) {
        if (receivedEventIds.contains(id)) {
            return;
        }
        EventReceived eventReceived = new EventReceived(id, date);
        if (receivedEventList.size() == 0) {
            receivedEventList.add(eventReceived);
        } else {
            int i = receivedEventList.size() - 1;
            while (i >= 0 && receivedEventList.get(i).date.compareTo(date) > 0) {
                i--;
            }
            receivedEventList.add(i + 1, eventReceived);
        }
        receivedEventIds.add(id);
    }

    /**
     * Receive all events that have a lower date than to.
     *
     * @param to The date until which to remove events (exclusive)
     */
    public synchronized void remove(Date to) {
        while (receivedEventList.size() > 0 && receivedEventList.get(0).date.compareTo(to) < 0) {
            EventReceived removed = receivedEventList.remove(0);
            receivedEventIds.remove(removed.id);
        }
    }

    public void logSize(Logger log, String logPrefix) {
        log.debug("{}:  Event de-duplication list size: {} | {}", logPrefix, receivedEventIds.size(), receivedEventList.size());
    }

    /**
     * Check if an event has already been received based on it's id.
     *
     * @param id The id of the message.
     * @return
     */
    public synchronized boolean contains(String id) {
        return receivedEventIds.contains(id);
    }

    @AllArgsConstructor
    private class EventReceived {
        private String id;
        private Date date;
    }
}
