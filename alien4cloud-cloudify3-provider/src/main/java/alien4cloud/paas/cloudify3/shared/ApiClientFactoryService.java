package alien4cloud.paas.cloudify3.shared;

import static alien4cloud.utils.AlienUtils.safe;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.client.AsyncRestTemplate;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

import alien4cloud.dao.IGenericSearchDAO;
import alien4cloud.exception.NotFoundException;
import alien4cloud.paas.cloudify3.configuration.CloudConfiguration;
import alien4cloud.paas.cloudify3.shared.restclient.A4cLogClient;
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
    @Resource(name = "cfy-es-dao")
    private IGenericSearchDAO cfyEsDao;

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

        // Find if there is an existing client for this exact configuration (equals and hashcode are managed on the connection information).
        Registration registration = clientRegistrations.get(cloudConfiguration);
        if (registration != null) {
            return registration.apiClient;
        }

        // First configure the manager urls
        List<String> managerUrls = getAndValidateUrls(cloudConfiguration.getUrl(), "manager");
        List<String> logQueueUrls = getAndValidateUrls(cloudConfiguration.getLogQueueUrl(), "logQueue");

        // check the size of both provided url.
        if (managerUrls.size() != logQueueUrls.size()) {
            throw new PluginConfigurationException("The number of log queue and manager url provided should be equals.");
        }

        // This is a new connection configuration, let's create it
        AuthenticationInterceptor interceptor = new AuthenticationInterceptor();
        interceptor.setUserName(cloudConfiguration.getUserName());
        interceptor.setPassword(cloudConfiguration.getPassword());
        interceptor.setTenant(cloudConfiguration.getTenant());
        registration = new Registration();
        registration.cloudConfiguration = cloudConfiguration;
        registration.managerUrls = managerUrls;
        registration.logQueueUrls = logQueueUrls;
        registration.apiClient = new ApiClient(
                new ApiHttpClient(restTemplate, managerUrls, interceptor, cloudConfiguration.getFailOverRetry(), cloudConfiguration.getFailOverDelay()));

        clientRegistrations.put(cloudConfiguration, registration);
        return registration.apiClient;
    }

    private List<String> getAndValidateUrls(String urlsString, String type) throws PluginConfigurationException {
        Set<String> urls = Sets.newLinkedHashSet(); // to keep the order
        if (StringUtils.isNotBlank(urlsString)) {
            List<String> split = Lists.newArrayList(StringUtils.defaultString(urlsString).split(","));
            split.forEach(url -> urls.add(url.trim()));
        }

        // make sure there is at least one url
        if (urls.size() == 0) {
            throw new PluginConfigurationException("No " + type + " url(s) provided");
        }

        // validate the format
        for (String url : urls) {
            if (!UrlUtil.isValid(url)) {
                throw new PluginConfigurationException("Invalid " + type + " URL format: " + url);
            }
        }

        return Lists.newArrayList(urls);
    }

    public synchronized void register(final CloudConfiguration cloudConfiguration, final String consumerId, final IEventConsumer eventConsumer)
            throws URISyntaxException {
        Registration registration = clientRegistrations.get(cloudConfiguration);
        if (registration == null) {
            throw new NotFoundException("An api client must have been created for the cloud configuration first");
        }
        if (registration.eventServiceInstances == null) {
            // Create the event service instance that manage polling and dispatching of events.
            log.info("Creating a new event listeners for cloudify manager with url(s) {}; logQueueUrl(s) {}", cloudConfiguration.getUrl(),
                    cloudConfiguration.getLogQueueUrl());
            registration.eventServiceInstances = createEventServiceInstances(registration.logQueueUrls);
        } else {
            log.info("Register consumer {} for event listener on existing connection {}", consumerId, cloudConfiguration.getUrl());
        }

        for (EventServiceInstance eventServiceInstance : safe(registration.eventServiceInstances)) {
            eventServiceInstance.register(consumerId, eventConsumer);
        }
        clientRegistrationByConsumer.put(consumerId, registration);
    }

    public synchronized void unRegister(String consumerId) {
        Registration registration = clientRegistrationByConsumer.remove(consumerId);
        if (registration == null) {
            log.info("No registration found for orchestrator {}", consumerId);
            return;
        }

        List<EventServiceInstance> eventServiceInstances = registration.eventServiceInstances;
        if (eventServiceInstances == null) {
            log.info("Un-register consumer {} for manager {}: Manager was not registered.", consumerId, registration.cloudConfiguration.getUrl());
            return;
        }

        log.info("Un-register consumer {} for manager {}.", consumerId, registration.cloudConfiguration.getUrl());
        for (EventServiceInstance eventServiceInstance : eventServiceInstances) {
            Set<String> remaining = eventServiceInstance.unRegister(consumerId);
            if (hasRemainingConsumers(remaining)) {
                log.info("No more consumers for manager {}.", consumerId, registration.cloudConfiguration.getUrl());
                eventServiceInstance.preDestroy();
                clientRegistrations.remove(registration.cloudConfiguration);
            }
        }
    }

    protected List<EventServiceInstance> createEventServiceInstances(List<String> logQueueUrls) throws URISyntaxException {
        List<EventServiceInstance> eventServiceInstances = new ArrayList<>(logQueueUrls.size());
        for (String logQueueUrl : logQueueUrls) {
            // Rebuild the url to find the one of the logs
            URI logServiceUri = new URI(logQueueUrl);
            log.info("Register event client for url {}", logServiceUri.toString());
            A4cLogClient a4cLogClient = new A4cLogClient(restTemplate, cfyEsDao, logServiceUri.toString());
            eventServiceInstances.add(new EventServiceInstance(a4cLogClient, scheduler, pluginConfigurationHolder));
        }

        return eventServiceInstances;
    }

    protected boolean hasRemainingConsumers(Set<String> remainingConsumerIds) {
        return remainingConsumerIds.size() == 0;
    }

    @PreDestroy
    public synchronized void stopAllPollings() {
        for (Registration registration : clientRegistrations.values()) {
            log.info("Stopping all polling for manager {}; logQueues {}", registration.managerUrls, registration.logQueueUrls);
            for (EventServiceInstance eventServiceInstance : safe(registration.eventServiceInstances)) {
                eventServiceInstance.preDestroy();
            }
        }
    }

    private class Registration {
        private ApiClient apiClient;
        private List<String> managerUrls;
        private List<String> logQueueUrls;
        private List<EventServiceInstance> eventServiceInstances;
        private CloudConfiguration cloudConfiguration;
    }

}