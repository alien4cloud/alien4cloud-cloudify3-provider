package alien4cloud.paas.cloudify3.configuration;

import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import alien4cloud.orchestrators.plugin.IOrchestratorPlugin;
import alien4cloud.paas.cloudify3.error.BadConfigurationException;
import alien4cloud.paas.cloudify3.model.Version;
import alien4cloud.paas.cloudify3.service.event.EventService;
import alien4cloud.paas.cloudify3.shared.ApiClientFactoryService;
import alien4cloud.paas.cloudify3.shared.restclient.ApiClient;
import alien4cloud.paas.cloudify3.shared.restclient.auth.SSLContextManager;
import alien4cloud.paas.exception.PluginConfigurationException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Component("cloudify-configuration-holder")
@Slf4j
public class CfyConnectionManager {
    @Inject
    private SSLContextManager sslContextManager;
    @Inject
    private ApiClientFactoryService apiClientFactoryService;
    @Inject
    private IOrchestratorPlugin orchestratorPlugin;
    @Inject
    private EventService eventService;

    private String orchestratorId;
    private CloudConfiguration configuration;
    @Getter
    private ApiClient apiClient;

    public CloudConfiguration getConfiguration() {
        if (configuration == null) {
            throw new BadConfigurationException("Plugin is not properly configured");
        } else {
            return configuration;
        }
    }

    public synchronized void setConfiguration(String orchestratorId, CloudConfiguration configuration) throws PluginConfigurationException {
        this.orchestratorId = orchestratorId;
        this.configuration = configuration;

        try {
            // Update the client
            if (this.apiClient != null) {
                // un register
                apiClientFactoryService.unRegister(orchestratorId);
            }
            apiClient = apiClientFactoryService.createOrGet(this.configuration);
            sslContextManager.disableSSLVerification(configuration.getDisableSSLVerification() != null && configuration.getDisableSSLVerification());
            Version version = apiClient.getVersionClient().read();
            long numberOfBlueprints = apiClient.getBlueprintClient().count();
            long numberOfDeployments = apiClient.getDeploymentClient().count();
            log.info("Configured PaaS provider for Cloudify version " + version.getVersion() + ". Manager has: " + numberOfBlueprints + " uploaded blueprint, "
                    + numberOfDeployments + " active deployments.");

            apiClientFactoryService.register(this.configuration, orchestratorId, eventService);
        } catch (Exception e) {
            log.error("Cloud configuration is not correct", e);
            throw new PluginConfigurationException("Cloud configuration is not correct", e);
        }
    }

    @PreDestroy
    public synchronized void destroy() {
        log.info("Orchestrator teardown {}", orchestratorId);
        apiClientFactoryService.unRegister(orchestratorId);
    }
}
