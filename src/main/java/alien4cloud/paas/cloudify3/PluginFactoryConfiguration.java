package alien4cloud.paas.cloudify3;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import alien4cloud.paas.cloudify3.service.ArtifactRegistryService;
import alien4cloud.paas.cloudify3.service.OrchestratorDeploymentPropertiesService;

@Configuration
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
}
