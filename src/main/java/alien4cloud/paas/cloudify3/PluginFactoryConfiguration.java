package alien4cloud.paas.cloudify3;

import alien4cloud.paas.cloudify3.error.CloudifyResponseErrorHandler;
import alien4cloud.paas.cloudify3.restclient.AsyncClientHttpRequestLogger;
import alien4cloud.paas.cloudify3.restclient.auth.AuthenticationInterceptor;
import alien4cloud.paas.cloudify3.service.ArtifactRegistryService;
import alien4cloud.paas.cloudify3.service.OrchestratorDeploymentPropertiesService;
import alien4cloud.paas.cloudify3.service.SchedulerServiceFactoryBean;
import alien4cloud.paas.cloudify3.shared.EventClient;
import alien4cloud.paas.cloudify3.shared.PluginConfigurationHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.*;
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

@Configuration
@ComponentScan(basePackages = { "alien4cloud.paas.cloudify3.shared" })
@Slf4j
public class PluginFactoryConfiguration {

    /** A static final index to identify pools in case of multiple instances. */
    private static final AtomicInteger POOL_ID = new AtomicInteger(0);

//    @Inject
//    private PluginConfigurationHolder pluginConfigurationHolder;

    @Bean(name = "artifact-registry-service")
    public ArtifactRegistryService artifactRegistryService() {
        return new ArtifactRegistryService();
    }

    @Bean(name = "cloudify-orchestrator")
    public CloudifyOrchestratorFactory cloudifyOrchestratorFactory() {
        return new CloudifyOrchestratorFactory();
    }

    @Bean(name = "deployment-properties-service")
    public OrchestratorDeploymentPropertiesService deploymentPropertiesService() {
        return new OrchestratorDeploymentPropertiesService();
    }

    @Bean(name = "shared-authentication-interceptor")
    public AuthenticationInterceptor authenticationInterceptor() {
        // just a bean to satisfy EventClient requirement
        // and to avoid scan of alien4cloud.paas.cloudify3 rather than alien4cloud.paas.cloudify3.shared
        return new AuthenticationInterceptor();
    }

//    @Bean(name = "live-event-client")
//    public EventClient liveEventClient() {
//        // Object mapper configuration
//        EventClient eventClient = new EventClient();
//        eventClient.setSpecificRestTemplate(asyncRestTemplate());
//        return eventClient;
//    }

    @Bean(name= "cloudify-async-thread-pool")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor(){
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setThreadNamePrefix("cloudify-async-thread-pool-" + POOL_ID.incrementAndGet() + "-");
        threadPoolTaskExecutor.setCorePoolSize(30);
        threadPoolTaskExecutor.setMaxPoolSize(50);
        threadPoolTaskExecutor.setKeepAliveSeconds(10);
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }

    @Bean(name = "cloudify-rest-template")
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
//        if (log.isTraceEnabled()) {
//            syncRestTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(simpleClientHttpRequestFactory()));
//        }
        return syncRestTemplate;
    }

    @Bean(name = "cloudify-async-http-request-factory")
    public SimpleClientHttpRequestFactory simpleClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        simpleClientHttpRequestFactory.setTaskExecutor(threadPoolTaskExecutor());
        return simpleClientHttpRequestFactory;
    }

    @Bean(name = "cloudify-async-rest-template")
    public AsyncRestTemplate asyncRestTemplate() {
        AsyncRestTemplate asyncRestTemplate = new AsyncRestTemplate(simpleClientHttpRequestFactory(), restTemplate());
        if (log.isTraceEnabled()) {
            List<AsyncClientHttpRequestInterceptor> interceptors = Lists.newArrayList(new AsyncClientHttpRequestLogger());
            asyncRestTemplate.setInterceptors(interceptors);
        }
        return asyncRestTemplate;
    }

    @Bean(name = "cloudify-scheduler")
    public SchedulerServiceFactoryBean schedulerServiceFactoryBean() {
        return new SchedulerServiceFactoryBean("cloudify-scheduler", 4);
    }

    // Dedicated delayed pollers stuffs: EventClient, RequestFactory, Thread pools.

    @Bean(name = "delayed-rest-template")
    public RestTemplate restTemplateDelayed() {
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
        syncRestTemplate.setRequestFactory(simpleClientHttpRequestFactoryDelayed());
//        if (log.isTraceEnabled()) {
//            syncRestTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(simpleClientHttpRequestFactoryDelayed()));
//        }
        return syncRestTemplate;
    }

    /**
     * @return a dedicated event client for delayed pollers.
     */
    @Bean(name = "delayed-event-client")
    public EventClient delayedEventClient() {
        // Object mapper configuration
        EventClient eventClient = new EventClient();
        eventClient.setSpecificRestTemplate(asyncRestTemplateDelayed());
        return eventClient;
    }

    /**
     * @return a dedicated thread pool for delayed pollers.
     */
    @Bean(name= "delayed-async-thread-pool")
    public ThreadPoolTaskExecutor threadPoolTaskExecutorDelayed(){
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setThreadNamePrefix("delayed-async-thread-pool-" + POOL_ID.incrementAndGet() + "-");
        threadPoolTaskExecutor.setCorePoolSize(2);
        threadPoolTaskExecutor.setMaxPoolSize(2);
        threadPoolTaskExecutor.setKeepAliveSeconds(10);
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }

    /**
     * @return a dedicated request factory for delayed pollers.
     */
    @Bean(name = "delayed-async-http-request-factory")
    public SimpleClientHttpRequestFactory simpleClientHttpRequestFactoryDelayed() {
        SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        simpleClientHttpRequestFactory.setTaskExecutor(threadPoolTaskExecutorDelayed());
        return simpleClientHttpRequestFactory;
    }

    /**
     * @return a dedicated async REST template for delayed pollers.
     */
    @Bean(name = "delayed-async-rest-template")
    public AsyncRestTemplate asyncRestTemplateDelayed() {
        AsyncRestTemplate asyncRestTemplate = new AsyncRestTemplate(simpleClientHttpRequestFactoryDelayed(), restTemplateDelayed());
        if (log.isTraceEnabled()) {
            List<AsyncClientHttpRequestInterceptor> interceptors = Lists.newArrayList(new AsyncClientHttpRequestLogger());
            asyncRestTemplate.setInterceptors(interceptors);
        }
        return asyncRestTemplate;
    }

    /**
     * @return a dedicated scheduler for delayed pollers.
     */
    @Bean(name = "delayed-scheduler")
    public SchedulerServiceFactoryBean schedulerServiceFactoryBeanDelayed() {
        return new SchedulerServiceFactoryBean("delayed-scheduler", 2);
    }

}
