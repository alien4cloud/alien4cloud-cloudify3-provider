package alien4cloud.paas.cloudify3.shared;

import java.util.Map;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import alien4cloud.paas.cloudify3.configuration.CloudConfiguration;
import alien4cloud.paas.cloudify3.restclient.auth.AuthenticationInterceptor;
import com.google.common.collect.Maps;
import org.springframework.stereotype.Service;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;

import lombok.extern.slf4j.Slf4j;

/**
 * This instance is responsible to manage events services per cloudify url rather than per orchestrator instance.
 */
@Slf4j
@Service
public class EventServiceMultiplexer {
    @Resource
    private EventClient logEventClient;

    @Resource(name = "cloudify-scheduler")
    private ListeningScheduledExecutorService scheduler;

    @Resource
    private PluginConfigurationHolder pluginConfigurationHolder;

    /** Map of event services by manager url. */
    private Map<String, EventServiceInstance> eventServices = Maps.newHashMap();

    // When a new one registers with a previous date then we have to re-poll older events but not dispatch events to listener that already processed them...

    public synchronized void register(final String managerUrl, final String username, final String password, String consumerId, IEventConsumer eventConsumer) {
        // Register a new authentication interceptor for the manager (this will not be taken in account if one is already registered).
        AuthenticationInterceptor interceptor = new AuthenticationInterceptor();
        interceptor.setUserName(username);
        interceptor.setPassword(password);
        logEventClient.registerAuthenticationManager(managerUrl, interceptor);

        EventServiceInstance logEventServiceInstance = eventServices.get(managerUrl);
        if (logEventServiceInstance == null) {
            log.info("Creating a new event listener for cloudify manager with url {}", managerUrl);
            logEventServiceInstance = newEventServiceInstance(managerUrl);
            eventServices.put(managerUrl, logEventServiceInstance);
        }
        log.info("Register consumer {} for event listener on manager {}", consumerId, managerUrl);
        logEventServiceInstance.register(consumerId, eventConsumer);
    }

    public synchronized void unRegister(final String managerUrl, String consumerId) {
        EventServiceInstance eventServiceInstance = eventServices.get(managerUrl);
        if (eventServiceInstance == null) {
            log.info("Un-register consumer {} for manager {}: Manager was not registered.", consumerId, managerUrl);
            return;
        }

        log.info("Un-register consumer {} for manager {}.", consumerId, managerUrl);
        Set<String> remaining = eventServiceInstance.unRegister(consumerId);
        if (hasRemaningConsumers(remaining)) {
            log.info("No more consumers for manager {}.", consumerId, managerUrl);
            eventServiceInstance.preDestroy();
        }
    }

    protected EventServiceInstance newEventServiceInstance(final String managerUrl) {
        return new EventServiceInstance(managerUrl, logEventClient, scheduler, pluginConfigurationHolder);
    }

    protected boolean hasRemaningConsumers(Set<String> remainingConsumerIds) {
        return remainingConsumerIds.size() == 0;
    }

    @PreDestroy
    public synchronized void stopAllPollings() {
        for (EventServiceInstance eventServiceInstance : eventServices.values()) {
            eventServiceInstance.preDestroy();
        }
    }
}