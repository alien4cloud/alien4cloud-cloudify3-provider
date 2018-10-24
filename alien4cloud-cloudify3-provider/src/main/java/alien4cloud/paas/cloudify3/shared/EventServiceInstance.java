package alien4cloud.paas.cloudify3.shared;

import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import alien4cloud.paas.cloudify3.eventpolling.*;
import alien4cloud.paas.cloudify3.service.SchedulerServiceFactoryBean;
import alien4cloud.paas.cloudify3.shared.restclient.EventClient;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Instance that polls events.
 */
@Slf4j
@Getter
public class EventServiceInstance {

    /**
     * Context
     */
    private final AnnotationConfigApplicationContext context;

    /**
     * Rest client
     */
    private final EventClient client;

    /**
     * Url
     */
    private final String url;

    /**
     * EventDispatcher
     *
     * @param context
     * @param url
     * @param client
     * @return
     */
    private final EventDispatcher eventDispatcher;

    EventServiceInstance(AnnotationConfigApplicationContext context,String url,EventClient client) {
        this.context = context;
        this.client = client;
        this.url = url;

        EventCache eventCache = (EventCache) context.getBean("event-cache");
        eventCache.setUrl(url);

        LivePoller livePoller = (LivePoller) context.getBean("event-live-poller");
        livePoller.setUrl(url);
        livePoller.setEventClient(client);

        DelayedPoller delayed30 = (DelayedPoller) context.getBean("event-delayed-30-poller");
        delayed30.setEventClient(client);
        DelayedPoller delayed300 = (DelayedPoller) context.getBean("event-delayed-300-poller");
        delayed300.setEventClient(client);

        livePoller.addDelayedPoller(delayed30);
        livePoller.addDelayedPoller(delayed300);

        RecoveryPoller recoveryPoller = (RecoveryPoller) context.getBean("event-recovery-poller");
        recoveryPoller.setUrl(url);
        recoveryPoller.setEventClient(client);

        eventDispatcher = (EventDispatcher) context.getBean("event-dispatcher");

        // Initialize the event cache
        eventCache.init();

        // Start the recovery poller
        recoveryPoller.start();

        // Start the live poller
        livePoller.start();
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
    }

    public synchronized Set<String> unRegister(String consumerId) {
        return eventDispatcher.unRegister(consumerId);
    }

    public void preDestroy() {
        context.destroy();
    }
}