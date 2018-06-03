package alien4cloud.paas.cloudify3;

import alien4cloud.paas.cloudify3.CloudifyOrchestratorFactory;
import alien4cloud.paas.cloudify3.error.CloudifyResponseErrorHandler;
import alien4cloud.paas.cloudify3.eventpolling.DelayedPoller;
import alien4cloud.paas.cloudify3.eventpolling.EventCache;
import alien4cloud.paas.cloudify3.eventpolling.HistoricPoller;
import alien4cloud.paas.cloudify3.eventpolling.LivePoller;
import alien4cloud.paas.cloudify3.restclient.AsyncClientHttpRequestLogger;
import alien4cloud.paas.cloudify3.restclient.auth.AuthenticationInterceptor;
import alien4cloud.paas.cloudify3.service.ArtifactRegistryService;
import alien4cloud.paas.cloudify3.service.OrchestratorDeploymentPropertiesService;
import alien4cloud.paas.cloudify3.service.SchedulerServiceFactoryBean;
import alien4cloud.paas.cloudify3.shared.EventClient;
import alien4cloud.paas.cloudify3.shared.EventDispatcher;
import alien4cloud.paas.cloudify3.shared.PluginConfigurationHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.AsyncClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The configuration for the cfy manager dedicated child context.
 * For each instance of manager url, we'll instanciate a manager context.
 */
@Configuration
@ComponentScan(basePackages = { "alien4cloud.paas.cloudify3.eventpolling" })
@Slf4j
public class CloudifyManagerCtxConfig {

    /** A static final index to identify pools in case of multiple instances. */
    private static final AtomicInteger POOL_ID = new AtomicInteger(0);

    @Inject
    private PluginConfigurationHolder pluginConfigurationHolder;

    @Bean(name = "event-dispatcher")
    public EventDispatcher eventDispatcher() {
        return new EventDispatcher();
    }

    @Bean(name = "event-cache")
    @SneakyThrows
    public EventCache eventCache() {
        EventCache eventCache = new EventCache();
        eventCache.setScheduler(schedulerServiceFactoryBean().getObject());
        return eventCache;
    }

    @Bean(name = "event-client")
    public EventClient liveEventClient() {
        // Object mapper configuration
        EventClient eventClient = new EventClient();
        eventClient.setSpecificRestTemplate(asyncRestTemplate());
        return eventClient;
    }

    @Bean(name = "event-live-poller")
    @SneakyThrows
    public LivePoller livePoller() throws Exception {
        LivePoller livePoller = new LivePoller();
        livePoller.setEventCache(eventCache());
        livePoller.setEventClient(liveEventClient());
        livePoller.setEventDispatcher(eventDispatcher());

        ListeningScheduledExecutorService executorService = schedulerServiceFactoryBean().getObject();

        // TODO: make this configurable, we should be able to configure delayed pollers per cfy manager.
        // Instanciate and set delayed pollers
        DelayedPoller delayed30sPoller = new DelayedPoller(30);
        delayed30sPoller.setEventCache(eventCache());
        delayed30sPoller.setEventClient(liveEventClient());
        delayed30sPoller.setEventDispatcher(eventDispatcher());
        delayed30sPoller.setScheduler(executorService);
        livePoller.addDelayedPoller(delayed30sPoller);

        DelayedPoller delayed5mnPoller = new DelayedPoller(60 * 5);
        delayed5mnPoller.setEventCache(eventCache());
        delayed5mnPoller.setEventClient(liveEventClient());
        delayed5mnPoller.setEventDispatcher(eventDispatcher());
        delayed5mnPoller.setScheduler(executorService);
        livePoller.addDelayedPoller(delayed5mnPoller);

        return livePoller;
    }

    @Bean(name = "event-historic-poller")
    public HistoricPoller historicPoller() {
        HistoricPoller historicPoller = new HistoricPoller();
        historicPoller.setEventCache(eventCache());
        historicPoller.setEventClient(liveEventClient());
        historicPoller.setEventDispatcher(eventDispatcher());
        return historicPoller;
    }

    /**
     * FIXME: when this thread is really used ? Does the fact that polls are blocking means this thread pool is not used ?
     * The thead pool for handling REST responses from events endpoint.
     * This thread pool doesn't need to be configurable since we know exactly how many thread we need:
     * <ul>
     *     <li>1 thread for live stream poller REST handling.</li>
     *     <li>2 threads for delayed event stream and historical event stream, REST handling.</li>
     * </ul>
     * @return
     */
    @Bean(name= "event-async-thread-pool")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor(){
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setThreadNamePrefix("event-async-thread-pool-" + POOL_ID.incrementAndGet() + "-");
        threadPoolTaskExecutor.setCorePoolSize(2);
        threadPoolTaskExecutor.setMaxPoolSize(3);
        threadPoolTaskExecutor.setKeepAliveSeconds(10);
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }

    @Bean(name = "event-rest-template")
    public RestTemplate restTemplate() {
        // Object mapper configuration
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
        objectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

        MappingJackson2HttpMessageConverter jackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        jackson2HttpMessageConverter.setObjectMapper(objectMapper);

        // Configure message converters for rest template
        List<HttpMessageConverter<?>> messageConverters = Lists.newArrayList();
        messageConverters.add(new ByteArrayHttpMessageConverter());
        messageConverters.add(new ResourceHttpMessageConverter());
        messageConverters.add(jackson2HttpMessageConverter);

        // Sync rest template
        RestTemplate syncRestTemplate = new RestTemplate();
        syncRestTemplate.setErrorHandler(new CloudifyResponseErrorHandler());
        syncRestTemplate.setMessageConverters(messageConverters);
        syncRestTemplate.setRequestFactory(simpleClientHttpRequestFactory());
        return syncRestTemplate;
    }

    @Bean(name = "event-async-http-request-factory")
    public SimpleClientHttpRequestFactory simpleClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        simpleClientHttpRequestFactory.setTaskExecutor(threadPoolTaskExecutor());
        return simpleClientHttpRequestFactory;
    }

    @Bean(name = "event-async-rest-template")
    public AsyncRestTemplate asyncRestTemplate() {
        AsyncRestTemplate asyncRestTemplate = new AsyncRestTemplate(simpleClientHttpRequestFactory(), restTemplate());
        if (log.isTraceEnabled()) {
            List<AsyncClientHttpRequestInterceptor> interceptors = Lists.newArrayList(new AsyncClientHttpRequestLogger());
            asyncRestTemplate.setInterceptors(interceptors);
        }
        return asyncRestTemplate;
    }

    /**
     * Used by:
     * <ul>
     *     <li>The {@link EventCache} to manage TTL</li>
     *     <li>The 2 delayed pollers to schedule delayed epoch polling (blocking)</li>
     *     <li>The {@link HistoricPoller} to submit the history epoch polling (blocking and potencially long run !)</li>
     * </ul>
     * @return
     */
    @Bean(name = "event-scheduler")
    public SchedulerServiceFactoryBean schedulerServiceFactoryBean() {
        return new SchedulerServiceFactoryBean("event-scheduler", 3);
    }

}
