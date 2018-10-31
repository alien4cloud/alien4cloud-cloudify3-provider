package alien4cloud.paas.cloudify3;

import alien4cloud.paas.cloudify3.error.CloudifyResponseErrorHandler;
import alien4cloud.paas.cloudify3.restclient.AsyncClientHttpRequestLogger;
import alien4cloud.paas.cloudify3.restclient.auth.AuthenticationInterceptor;
import alien4cloud.paas.cloudify3.restclient.auth.SSLContextManager;
import alien4cloud.paas.cloudify3.service.ArtifactRegistryService;
import alien4cloud.paas.cloudify3.service.OrchestratorDeploymentPropertiesService;
import alien4cloud.paas.cloudify3.service.PluginArchiveService;
import alien4cloud.paas.cloudify3.service.SchedulerServiceFactoryBean;
import alien4cloud.paas.cloudify3.util.SyspropConfig;
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

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@ComponentScan(basePackages = {"alien4cloud.paas.cloudify3.shared"})
@Slf4j
public class PluginFactoryConfiguration {

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

}
