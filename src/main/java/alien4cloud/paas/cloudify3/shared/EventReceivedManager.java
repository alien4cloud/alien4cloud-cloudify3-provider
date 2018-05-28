package alien4cloud.paas.cloudify3.shared;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Manages a list of received events to avoid duplicated events when looking for missing ones.
 */
public class EventReceivedManager {
    private List<EventReceived> receivedEventList = Lists.newLinkedList();
    private Set<String> receivedEventIds = Sets.newHashSet();

    /** A lock to avoid multithreaded access to resources : receivedEventList & receivedEventIds. */
    private final ReentrantReadWriteLock resourceLock = new ReentrantReadWriteLock();

    /**
     * Add an event in the list of received events.
     */
    public void addEvent(String id, Date date) {
        resourceLock.writeLock().lock();
        try {
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
        } finally {
            resourceLock.writeLock().unlock();
        }
    }

    /**
     * Receive all events that have a lower date than to.
     *
     * @param to The date until which to remove events (exclusive)
     */
    public void remove(Date to) {
        resourceLock.writeLock().lock();
        try {
            while (receivedEventList.size() > 0 && receivedEventList.get(0).date.compareTo(to) < 0) {
                EventReceived removed = receivedEventList.remove(0);
                receivedEventIds.remove(removed.id);
            }
        } finally {
            resourceLock.writeLock().unlock();
        }
    }

    public void logSize(Logger log, String logPrefix) {
        resourceLock.readLock().lock();
        try {
            log.debug("{}:  Event de-duplication list size: {} | {}", logPrefix, receivedEventIds.size(), receivedEventList.size());
        } finally {
            resourceLock.readLock().unlock();
        }
    }

    /**
     * Check if an event has already been received based on it's id.
     *
     * @param id The id of the message.
     * @return
     */
    public boolean contains(String id) {
        resourceLock.readLock().lock();
        try {
            return receivedEventIds.contains(id);
        } finally {
            resourceLock.readLock().unlock();
        }
    }

    @AllArgsConstructor
    private class EventReceived {
        private String id;
        private Date date;
    }
}
