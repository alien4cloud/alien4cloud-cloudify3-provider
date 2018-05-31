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

    }

    /**
     * Receive all events that have a lower date than to.
     *
     * @param to The date until which to remove events (exclusive)
     */
    public void remove(Date to) {

    }

    public void logSize(Logger log, String logPrefix) {

    }

    /**
     * Check if an event has already been received based on it's id.
     *
     * @param id The id of the message.
     * @return
     */
    public boolean contains(String id) {
        return false;
    }

    @AllArgsConstructor
    private class EventReceived {
        private String id;
        private Date date;
    }
}
