package alien4cloud.paas.cloudify3;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.Lists;

import alien4cloud.paas.cloudify3.error.CloudifyResponseErrorHandler;
import alien4cloud.paas.cloudify3.service.OrchestratorDeploymentPropertiesService;
import alien4cloud.paas.cloudify3.service.SchedulerServiceFactoryBean;

@Configuration
@ComponentScan(basePackages = { "alien4cloud.paas.cloudify3.shared" })
public class PluginFactoryConfiguration {
    @Bean(name = "cloudify-orchestrator")
    public CloudifyOrchestratorFactory cloudifyOrchestratorFactory() {
        return new CloudifyOrchestratorFactory();
    }

    @Bean(name = "deployment-properties-service")
    public OrchestratorDeploymentPropertiesService deploymentPropertiesService() {
        return new OrchestratorDeploymentPropertiesService();
    }

    @Bean(name = "cloudify-async-thread-pool")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setThreadNamePrefix("cloudify-async-thread-pool");
        threadPoolTaskExecutor.setCorePoolSize(5);
        threadPoolTaskExecutor.setMaxPoolSize(Integer.MAX_VALUE);
        threadPoolTaskExecutor.setKeepAliveSeconds(10);
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }

    @Bean(name = "cloudify-async-http-request-factory")
    public SimpleClientHttpRequestFactory simpleClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        simpleClientHttpRequestFactory.setTaskExecutor(threadPoolTaskExecutor());
        return simpleClientHttpRequestFactory;
    }

    @Bean(name = "cloudify-rest-template")
    public RestTemplate restTemplate() {
        // Object mapper configuration
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

        MappingJackson2HttpMessageConverter jackson2HttpMessageConverter = new MappingJackson2HttpMessageConverter();
        jackson2HttpMessageConverter.setObjectMapper(objectMapper);

        // Configure message converters for rest template
        List<HttpMessageConverter<?>> messageConverters = Lists.newArrayList();
        messageConverters.add(new ByteArrayHttpMessageConverter());
        messageConverters.add(new ResourceHttpMessageConverter());
        messageConverters.add(jackson2HttpMessageConverter);

        // Sync rest template
        RestTemplate syncRestTemplate = new RestTemplate(simpleClientHttpRequestFactory());
        syncRestTemplate.setErrorHandler(new CloudifyResponseErrorHandler());
        syncRestTemplate.setMessageConverters(messageConverters);
        return syncRestTemplate;
    }

    @Bean(name = "cloudify-async-rest-template")
    public AsyncRestTemplate asyncRestTemplate() {
        return new AsyncRestTemplate(simpleClientHttpRequestFactory(), restTemplate());
    }

    @Primary
    @Bean(name = "cloudify-scheduler")
    public SchedulerServiceFactoryBean schedulerServiceFactoryBean() {
        return new SchedulerServiceFactoryBean();
    }
}
