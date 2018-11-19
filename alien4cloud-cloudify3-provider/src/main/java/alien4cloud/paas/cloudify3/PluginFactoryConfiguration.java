package alien4cloud.paas.cloudify3;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import alien4cloud.paas.cloudify3.service.OrchestratorDeploymentPropertiesService;

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

}
