package alien4cloud.paas.cloudify3.shared;

import alien4cloud.paas.cloudify3.CloudifyManagerCtxConfig;
import alien4cloud.paas.cloudify3.eventpolling.EventCache;
import alien4cloud.paas.cloudify3.eventpolling.RecoveryPoller;
import alien4cloud.paas.cloudify3.eventpolling.LivePoller;
import alien4cloud.paas.cloudify3.restclient.auth.AuthenticationInterceptor;
import alien4cloud.utils.ClassLoaderUtil;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;

/**
 * This instance is responsible to manage events polling mecanism per cloudify url rather than per orchestrator instance.
 */
@Slf4j
@Service
public class EventServiceMultiplexer {

    @Resource
    private ApplicationContext mainContext;

    @Resource
    private PluginConfigurationHolder pluginConfigurationHolder;

    @Resource
    private LogEventConsumer logEventConsumer;

    /** manager url -> manager dedicated context */
    private Map<String, AnnotationConfigApplicationContext> managerContexts = Maps.newHashMap();

    // When a new one registers with a previous date then we have to re-poll older events but not dispatch events to listener that already processed them...

    public synchronized void register(final String managerUrl, final String username, final String password, String consumerId, IEventConsumer eventConsumer) {

        AnnotationConfigApplicationContext managerContext = managerContexts.get(managerUrl);
        boolean contextCreated = false;
        if (managerContext == null) {
            log.info("No context found for manager " + managerUrl);
            managerContext = startContext(managerUrl, username, password);
            managerContexts.put(managerUrl, managerContext);
            contextCreated = true;
        }
        EventDispatcher eventDispatcher = (EventDispatcher) managerContext.getBean("event-dispatcher");
        eventDispatcher.register(consumerId, eventConsumer);
        if (contextCreated) {
            eventDispatcher.register(LogEventConsumer.LOG_EVENT_CONSUMER_ID, logEventConsumer);
        }
    }

    private AnnotationConfigApplicationContext startContext(String managerUrl, final String username, final String password) {
        AnnotationConfigApplicationContext managerContext = new AnnotationConfigApplicationContext();
        managerContext.setParent(mainContext);
        managerContext.setClassLoader(mainContext.getClassLoader());
        ClassLoaderUtil.runWithContextClassLoader(mainContext.getClassLoader(), () -> {
            managerContext.register(CloudifyManagerCtxConfig.class);
            managerContext.refresh();
        });
        log.info("Created new Cloudify manager context {} for factory {}, managing cfy manager @{}", managerContext.getId(), mainContext.getId(), managerUrl);

        // setup the authentication interceptor so the client can authenticate to the cfy manager
        AuthenticationInterceptor authenticationInterceptor = new AuthenticationInterceptor();
        authenticationInterceptor.setUserName(username);
        authenticationInterceptor.setPassword(password);
        EventClient eventClient = (EventClient) managerContext.getBean("event-client");
        eventClient.setAuthenticationInterceptor(authenticationInterceptor);

        EventCache eventCache = (EventCache) managerContext.getBean("event-cache");
        eventCache.setUrl(managerUrl);
        LivePoller livePoller = (LivePoller) managerContext.getBean("event-live-poller");
        livePoller.setUrl(managerUrl);
        RecoveryPoller recoveryPoller = (RecoveryPoller) managerContext.getBean("event-recovery-poller");
        recoveryPoller.setUrl(managerUrl);

        // start the event cache
        eventCache.init();
        // start the historical ...
        recoveryPoller.start();
        // ... then the live
        // even if we are asynchronous, I prefer to be sure that the historic poller will get a thread before delayed.
        livePoller.start();

        return managerContext;
    }

    public synchronized void unRegister(final String managerUrl, String consumerId) {
        AnnotationConfigApplicationContext managerContext = managerContexts.get(managerUrl);
        if (managerContext == null) {
            log.info("Un-register consumer {} for manager {}: Manager was not registered.", consumerId, managerUrl);
            return;
        }

        log.info("Un-register consumer {} for manager {}.", consumerId, managerUrl);
        EventDispatcher eventDispatcher = (EventDispatcher) managerContext.getBean("event-dispatcher");

        Set<String> remaining = eventDispatcher.unRegister(consumerId);
        if (remaining.size() == 1 && remaining.stream().findFirst().get().equals(LogEventConsumer.LOG_EVENT_CONSUMER_ID)) {
            eventDispatcher.unRegister(LogEventConsumer.LOG_EVENT_CONSUMER_ID);
            log.info("No more consumers for manager {}.", managerUrl);
            managerContext.destroy();
            managerContexts.remove(managerUrl);
        }
    }

    protected boolean hasRemaningConsumers(Set<String> remainingConsumerIds) {
        return remainingConsumerIds.size() == 0;
    }

    @PreDestroy
    public synchronized void shutdown() {
        managerContexts.forEach((s, ctx) -> ctx.destroy());
    }
}