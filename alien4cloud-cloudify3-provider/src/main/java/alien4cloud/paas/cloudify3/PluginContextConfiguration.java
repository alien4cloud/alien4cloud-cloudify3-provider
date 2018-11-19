package alien4cloud.paas.cloudify3;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import alien4cloud.paas.cloudify3.util.SyspropConfig;
import org.springframework.boot.actuate.autoconfigure.ManagementServerProperties;
import org.springframework.context.annotation.*;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.Lists;

import alien4cloud.paas.cloudify3.error.CloudifyResponseErrorHandler;
import alien4cloud.paas.cloudify3.service.SchedulerServiceFactoryBean;

@EnableGlobalMethodSecurity(prePostEnabled = true)
@Order(ManagementServerProperties.ACCESS_OVERRIDE_ORDER)
@Configuration
@ComponentScan(basePackages = { "alien4cloud.paas.cloudify3" }, excludeFilters = {
        @Filter(type = FilterType.REGEX, pattern = "alien4cloud\\.paas\\.cloudify3\\.shared\\..*"),
        @Filter(type = FilterType.REGEX, pattern = "alien4cloud\\.paas\\.cloudify3\\eventpolling\\..*") })
@ImportResource("classpath:plugin-properties-config.xml")
public class PluginContextConfiguration {

    private static final AtomicInteger POOL_ID = new AtomicInteger(0);

    @Bean(name = "cloudify-async-thread-pool")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();

        threadPoolTaskExecutor.setThreadNamePrefix("cloudify-async-thread-pool-" + POOL_ID.incrementAndGet() + "-");
        threadPoolTaskExecutor.setCorePoolSize(SyspropConfig.getInt(SyspropConfig.CLOUDIFY_ASYNC_CORE_SIZE, 30));
        threadPoolTaskExecutor.setMaxPoolSize(SyspropConfig.getInt(SyspropConfig.CLOUDIFY_ASYNC_MAX_SIZE, 50));
        threadPoolTaskExecutor.setKeepAliveSeconds(SyspropConfig.getInt(SyspropConfig.CLOUDIFY_ASYNC_KEEPALIVESECONDS, 10));
        threadPoolTaskExecutor.initialize();

        return threadPoolTaskExecutor;
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

    @Bean(name = "cloudify-async-http-request-factory")
    public SimpleClientHttpRequestFactory simpleClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();

        simpleClientHttpRequestFactory.setTaskExecutor(threadPoolTaskExecutor());
        simpleClientHttpRequestFactory.setConnectTimeout(SyspropConfig.getInt(SyspropConfig.CLOUDIFY_CONNECT_TIMEOUT, 20000));
        simpleClientHttpRequestFactory.setReadTimeout(SyspropConfig.getInt(SyspropConfig.CLOUDIFY_READ_TIMEOUT, 120000));

        return simpleClientHttpRequestFactory;
    }

    @Bean(name = "cloudify-async-rest-template")
    public AsyncRestTemplate asyncRestTemplate() {
        return new AsyncRestTemplate(simpleClientHttpRequestFactory(), restTemplate());
    }

    @Primary
    @Bean(name = "cloudify-scheduler")
    public SchedulerServiceFactoryBean schedulerServiceFactoryBean() {
        return new SchedulerServiceFactoryBean("cloudify-scheduler", SyspropConfig.getInt(SyspropConfig.CLOUDIFY_SCHEDULER_CORE_SIZE, 4));
    }
}