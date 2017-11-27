package alien4cloud.paas.cloudify3.util;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.SettableFuture;

import alien4cloud.deployment.model.SecretProviderConfigurationAndCredentials;
import alien4cloud.model.deployment.DeploymentTopology;
import alien4cloud.model.orchestrators.locations.Location;
import alien4cloud.model.secret.SecretProviderConfiguration;
import alien4cloud.paas.IPaaSCallback;
import alien4cloud.paas.cloudify3.CloudifyOrchestrator;
import alien4cloud.paas.cloudify3.configuration.CfyConnectionManager;
import alien4cloud.paas.cloudify3.configuration.CloudConfiguration;
import alien4cloud.paas.exception.PluginConfigurationException;
import alien4cloud.paas.model.PaaSTopologyDeploymentContext;
import alien4cloud.paas.plan.TopologyTreeBuilderService;
import alien4cloud.utils.AlienConstants;

@Component
public class DeploymentLauncher {

    @Resource(name = "cfy3-applicationUtil")
    private ApplicationUtil applicationUtil;
    @Resource
    private TopologyTreeBuilderService topologyTreeBuilderService;
    @Resource
    private CloudifyOrchestrator cloudifyPaaSProvider;
    @Resource
    private CfyConnectionManager cloudConfigurationHolder;

    private boolean initialized = false;

    public synchronized void initializeCloudifyManagerConnection() throws PluginConfigurationException {
        // Lazily initialize cloudify connection as test does not need this
        // It's for people who use TestDeploymentService to perform manual testing
        if (initialized) {
            return;
        }
        CloudConfiguration cloudConfiguration = cloudConfigurationHolder.getConfiguration();
        String cloudifyURL = System.getenv("CLOUDIFY_URL");
        cloudConfiguration.setUrl(cloudifyURL);
        cloudConfiguration.setUserName("admin");
        cloudConfiguration.setPassword("admin");
        cloudConfiguration.setDisableSSLVerification(true);
        cloudConfigurationHolder.setConfiguration("orchestratorId", cloudConfiguration);
        initialized = true;
    }

    public PaaSTopologyDeploymentContext buildPaaSDeploymentContext(String appName, String topologyName, String locationName) {
        return buildPaaSDeploymentContext(appName, topologyName, locationName, null);
    }

    public PaaSTopologyDeploymentContext buildPaaSDeploymentContext(String appName, String topologyName, String locationName,
            Map<String, String> deploymentProperties) {
        DeploymentTopology deploymentTopology = applicationUtil.createAlienApplication(appName, topologyName, locationName);
        PaaSTopologyDeploymentContext deploymentContext = new PaaSTopologyDeploymentContext();
        deploymentContext.setPaaSTopology(topologyTreeBuilderService.buildPaaSTopology(deploymentTopology));
        deploymentContext.setDeploymentTopology(deploymentTopology);
        alien4cloud.model.deployment.Deployment deployment = new alien4cloud.model.deployment.Deployment();
        deployment.setId(appName);
        deployment.setOrchestratorDeploymentId(appName);
        deploymentContext.setDeployment(deployment);
        Map<String, Location> locationMap = Maps.newHashMap();
        Location location = new Location();
        location.setOrchestratorId("undefined");
        location.setInfrastructureType(locationName);
        locationMap.put(AlienConstants.GROUP_ALL, location);
        deploymentContext.setLocations(locationMap);
        deploymentTopology.setProviderDeploymentProperties(deploymentProperties);
        SecretProviderConfigurationAndCredentials secretProviderConfigurationAndCredentials = new SecretProviderConfigurationAndCredentials();
        secretProviderConfigurationAndCredentials.setSecretProviderConfiguration(new SecretProviderConfiguration());
        secretProviderConfigurationAndCredentials.getSecretProviderConfiguration().setPluginName("vault");
        secretProviderConfigurationAndCredentials.getSecretProviderConfiguration()
                .setConfiguration(ImmutableMap.builder().put("url", "https://localhost").put("authenticationMethod", "ldap").build());
        deploymentContext.setSecretProviderConfigurationAndCredentials(secretProviderConfigurationAndCredentials);
        return deploymentContext;
    }

    public void launch(PaaSTopologyDeploymentContext deploymentContext) throws Exception {
        initializeCloudifyManagerConnection();
        final SettableFuture<Object> future = SettableFuture.create();
        cloudifyPaaSProvider.deploy(deploymentContext, new IPaaSCallback<Object>() {

            @Override
            public void onSuccess(Object data) {
                future.set(data);
            }

            @Override
            public void onFailure(Throwable throwable) {
                future.setException(throwable);
            }
        });
        future.get();
    }

    public String launch(String topologyName) throws Exception {
        return launch(topologyName, null);
    }

    public String launch(String topologyName, Map<String, String> deploymentProperties) throws Exception {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        String deploymentId = stackTraceElements[2].getMethodName();
        launch(buildPaaSDeploymentContext(deploymentId, topologyName, "amazon", deploymentProperties));
        return deploymentId;
    }
}
