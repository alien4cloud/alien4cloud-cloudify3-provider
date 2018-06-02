package alien4cloud.paas.cloudify3.shared;

import alien4cloud.paas.cloudify3.CloudifyManagerCtxConfig;
import alien4cloud.paas.cloudify3.eventpolling.HistoricPoller;
import alien4cloud.paas.cloudify3.eventpolling.LivePoller;
import alien4cloud.paas.cloudify3.restclient.auth.AuthenticationInterceptor;
import alien4cloud.utils.ClassLoaderUtil;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
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

    /** manager url -> manager dedicated context */
    private Map<String, AnnotationConfigApplicationContext> managerContexts = Maps.newHashMap();

    // When a new one registers with a previous date then we have to re-poll older events but not dispatch events to listener that already processed them...

    public synchronized void register(final String managerUrl, final String username, final String password, String consumerId, IEventConsumer eventConsumer) {

        AnnotationConfigApplicationContext managerContext = managerContexts.get(managerUrl);
        if (managerContext == null) {
            managerContext = startContext(managerUrl, username, password);
            managerContexts.put(managerUrl, managerContext);
        }
        EventDispatcher eventDispatcher = (EventDispatcher) managerContext.getBean("event-dispatcher");
        eventDispatcher.register(consumerId, eventConsumer);
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

        LivePoller livePoller = (LivePoller) managerContext.getBean("event-live-poller");
        livePoller.setUrl(managerUrl);
        livePoller.start();
        HistoricPoller historicPoller = (HistoricPoller) managerContext.getBean("event-historic-poller");
        historicPoller.start();

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
        if (hasRemaningConsumers(remaining)) {
            log.info("No more consumers for manager {}.", consumerId, managerUrl);
            managerContext.destroy();
            // TODO remove from map
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