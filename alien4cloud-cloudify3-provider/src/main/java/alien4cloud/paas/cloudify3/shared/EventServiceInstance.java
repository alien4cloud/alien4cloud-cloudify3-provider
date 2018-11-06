package alien4cloud.paas.cloudify3.shared;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import alien4cloud.paas.cloudify3.configuration.CloudConfiguration;
import alien4cloud.paas.cloudify3.error.CloudifyResponseErrorHandler;
import alien4cloud.paas.cloudify3.eventpolling.*;

import alien4cloud.paas.cloudify3.shared.restclient.ApiHttpClient;
import alien4cloud.paas.cloudify3.shared.restclient.EventClient;

import alien4cloud.paas.cloudify3.shared.restclient.auth.AuthenticationInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.Lists;
import io.netty.channel.EventLoopGroup;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

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
     * EventDispatcher
     *
     * @param context
     * @param url
     * @param client
     * @return
     */
    private final EventDispatcher eventDispatcher;

    EventServiceInstance(AnnotationConfigApplicationContext context, String url, CloudConfiguration cloudConfiguration) {
        this.context = context;

        EventClient client = buildClient(url,cloudConfiguration);

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
     * Build the rest client we will use
     *
     * @param url
     * @param cloudConfiguration
     * @return
     */
    private EventClient buildClient(String url,CloudConfiguration cloudConfiguration) {
        // This is a new connection configuration, let's create it
        AuthenticationInterceptor interceptor = new AuthenticationInterceptor();
        interceptor.setUserName(cloudConfiguration.getUserName());
        interceptor.setPassword(cloudConfiguration.getPassword());
        interceptor.setTenant(cloudConfiguration.getTenant());

        EventLoopGroup eventLoopGroup = (EventLoopGroup) context.getBean("cloudify-event-loop");
        Netty4ClientHttpRequestFactory clientFactory = new Netty4ClientHttpRequestFactory(eventLoopGroup);

        RestTemplate template = new RestTemplate(clientFactory);
        template.setErrorHandler(new CloudifyResponseErrorHandler());
        template.setMessageConverters(buildConverters());
        template.setRequestFactory(clientFactory);

        ApiHttpClient apiHttpClient = new ApiHttpClient(
                new AsyncRestTemplate(clientFactory,template),
                Arrays.asList(url),
                interceptor,
                cloudConfiguration.getFailOverRetry(),
                cloudConfiguration.getFailOverDelay()
            );

        return new EventClient(apiHttpClient);
    }

    /**
     * Build message converters to use with the client
     */
    private List<HttpMessageConverter<?>> buildConverters() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

        MappingJackson2HttpMessageConverter jackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        jackson2HttpMessageConverter.setObjectMapper(objectMapper);

        List<HttpMessageConverter<?>> messageConverters = Lists.newArrayList();
        messageConverters.add(new ByteArrayHttpMessageConverter());
        messageConverters.add(new ResourceHttpMessageConverter());
        messageConverters.add(jackson2HttpMessageConverter);

        return messageConverters;
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

    /**
     * Unregister an event consumer.
     *
     * @param consumerId
     * @return
     */
    public synchronized Set<String> unRegister(String consumerId) {
        return eventDispatcher.unRegister(consumerId);
    }

    public void preDestroy() {
        context.destroy();
    }
}