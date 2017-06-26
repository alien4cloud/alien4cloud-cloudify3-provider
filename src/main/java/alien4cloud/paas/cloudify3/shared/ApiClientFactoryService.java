package alien4cloud.paas.cloudify3.shared;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.springframework.stereotype.Service;
import org.springframework.web.client.AsyncRestTemplate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

import alien4cloud.exception.NotFoundException;
import alien4cloud.paas.cloudify3.configuration.CloudConfiguration;
import alien4cloud.paas.cloudify3.shared.restclient.ApiClient;
import alien4cloud.paas.cloudify3.shared.restclient.ApiHttpClient;
import alien4cloud.paas.cloudify3.shared.restclient.auth.AuthenticationInterceptor;
import alien4cloud.paas.exception.PluginConfigurationException;
import alien4cloud.utils.UrlUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * This instance is responsible to manage events services per cloudify url rather than per orchestrator instance.
 */
@Slf4j
@Service
public class ApiClientFactoryService {
    @Resource(name = "cloudify-scheduler")
    private ListeningScheduledExecutorService scheduler;
    @Resource(name = "cloudify-async-rest-template")
    private AsyncRestTemplate restTemplate;
    @Resource
    private PluginConfigurationHolder pluginConfigurationHolder;

    /** Map of event services by manager url. */
    private Map<CloudConfiguration, Registration> clientRegistrations = Maps.newHashMap();
    private Map<String, Registration> clientRegistrationByConsumer = Maps.newHashMap();

    /**
     * Create or get an API client based on the given cloud configuration.
     *
     * @param cloudConfiguration The cloud configuration for which to get the api client.
     * @return An API client instance.
     * @throws PluginConfigurationException In case the url is not a valid url.
     */
    public synchronized ApiClient createOrGet(final CloudConfiguration cloudConfiguration) throws PluginConfigurationException {
        // First configure the manager urls
        List<String> managerUrls = getAndValidateManagerUrls(cloudConfiguration.getUrl());

        // Find if there is an existing client for this exact configuration (equals and hashcode are managed on the connection information).
        Registration registration = clientRegistrations.get(cloudConfiguration);
        if (registration != null) {
            return registration.apiClient;
        }

        // This is a new connection configuration, let's create it
        AuthenticationInterceptor interceptor = new AuthenticationInterceptor();
        interceptor.setUserName(cloudConfiguration.getUserName());
        interceptor.setPassword(cloudConfiguration.getPassword());
        interceptor.setTenant(cloudConfiguration.getTenant());
        registration = new Registration();
        registration.cloudConfiguration = cloudConfiguration;
        registration.apiClient = new ApiClient(
                new ApiHttpClient(restTemplate, managerUrls, interceptor, cloudConfiguration.getFailOverRetry(), cloudConfiguration.getFailOverDelay()));

        clientRegistrations.put(cloudConfiguration, registration);
        return registration.apiClient;
    }

    private List<String> getAndValidateManagerUrls(String managerUrlsString) throws PluginConfigurationException {
        List<String> split = Lists.newArrayList(managerUrlsString.split(","));
        Set<String> urls = Sets.newLinkedHashSet(); // to keep the order
        split.forEach(url -> urls.add(url.trim()));

        // make sure there is at least one url
        if (urls.size() == 0) {
            throw new PluginConfigurationException("No manager url(s) provided");
        }

        // validate the format
        for (String url : urls) {
            if (!UrlUtil.isValid(url)) {
                throw new PluginConfigurationException("Invalid manager URL format: " + url);
            }
        }

        return Lists.newArrayList(urls);
    }

    public synchronized void register(final CloudConfiguration cloudConfiguration, final String consumerId, final IEventConsumer eventConsumer) {
        Registration registration = clientRegistrations.get(cloudConfiguration);
        if (registration == null) {
            throw new NotFoundException("An api client must have been created for the cloud configuration first");
        }
        if (registration.eventServiceInstance == null) {
            // Create the event service instance that manage polling and dispatching of events.
            log.info("Creating a new event listener for cloudify manager with url {}", cloudConfiguration.getUrl());
            registration.eventServiceInstance = newEventServiceInstance(registration.apiClient);
        } else {
            log.info("Register consumer {} for event listener on existing connection {}", consumerId, cloudConfiguration.getUrl());
        }

        registration.eventServiceInstance.register(consumerId, eventConsumer);
        clientRegistrationByConsumer.put(consumerId, registration);
    }

    public synchronized void unRegister(String consumerId) {
        Registration registration = clientRegistrationByConsumer.remove(consumerId);
        if (registration == null) {
            log.info("No registration found for orchestrator {}", consumerId);
            return;
        }

        EventServiceInstance eventServiceInstance = registration.eventServiceInstance;
        if (eventServiceInstance == null) {
            log.info("Un-register consumer {} for manager {}: Manager was not registered.", consumerId, registration.cloudConfiguration.getUrl());
            return;
        }

        log.info("Un-register consumer {} for manager {}.", consumerId, registration.cloudConfiguration.getUrl());
        Set<String> remaining = eventServiceInstance.unRegister(consumerId);
        if (hasRemaningConsumers(remaining)) {
            log.info("No more consumers for manager {}.", consumerId, registration.cloudConfiguration.getUrl());
            eventServiceInstance.preDestroy();
            clientRegistrations.remove(registration.cloudConfiguration);
        }
    }

    protected EventServiceInstance newEventServiceInstance(final ApiClient apiClient) {
        return new EventServiceInstance(apiClient.getEventClient(), scheduler, pluginConfigurationHolder);
    }

    protected boolean hasRemaningConsumers(Set<String> remainingConsumerIds) {
        return remainingConsumerIds.size() == 0;
    }

    @PreDestroy
    public synchronized void stopAllPollings() {
        for (Registration registration : clientRegistrations.values()) {
            registration.eventServiceInstance.preDestroy();
        }
    }

    private class Registration {
        private ApiClient apiClient;
        private EventServiceInstance eventServiceInstance;
        private CloudConfiguration cloudConfiguration;
    }
}