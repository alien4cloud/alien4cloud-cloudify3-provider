package alien4cloud.paas.cloudify3;

import alien4cloud.model.components.PropertyDefinition;
import alien4cloud.model.orchestrators.ArtifactSupport;
import alien4cloud.model.orchestrators.locations.LocationSupport;
import alien4cloud.orchestrators.plugin.IOrchestratorPluginFactory;
import alien4cloud.paas.IPaaSProvider;
import alien4cloud.paas.cloudify3.configuration.CloudConfiguration;
import alien4cloud.paas.cloudify3.configuration.LocationConfiguration;
import alien4cloud.paas.cloudify3.configuration.LocationConfigurations;
import alien4cloud.paas.cloudify3.location.ITypeAwareLocationConfigurator;
import alien4cloud.paas.cloudify3.service.OrchestratorDeploymentPropertiesService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@Slf4j
public class CloudifyOrchestratorFactory implements IOrchestratorPluginFactory<CloudifyOrchestrator, CloudConfiguration> {

    public static final String CFY_DSL_1_3 = "cloudify_dsl_1_3";
    public static final String CFY_VERSION = "3.4";
    public static final String CFY_DIAMOND_VERSION = "1.3.1";
    public static final String CFY_AWS_PLUGIN_VERSION = "1.3.1";
    public static final String CFY_OPENSTACK_PLUGIN_VERSION = "1.3.1";
    public static final String CFY_BYON_PLUGIN_VERSION = "1.4";

    @Resource
    private ApplicationContext factoryContext;

    @Resource
    private OrchestratorDeploymentPropertiesService deploymentPropertiesService;

    private Map<IPaaSProvider, AnnotationConfigApplicationContext> contextMap = Collections
            .synchronizedMap(Maps.<IPaaSProvider, AnnotationConfigApplicationContext> newIdentityHashMap());

    @Override
    public Class<CloudConfiguration> getConfigurationType() {
        return CloudConfiguration.class;
    }

    @Override
    public CloudConfiguration getDefaultConfiguration() {
        CloudConfiguration cloudConfiguration = new CloudConfiguration();
        cloudConfiguration.setUrl("http://yourManagerIP");
        cloudConfiguration.setDisableSSLVerification(false);
        cloudConfiguration.setDelayBetweenDeploymentStatusPolling(30);
        cloudConfiguration.setDelayBetweenInProgressDeploymentStatusPolling(5);
        LocationConfigurations locationConfigurations = new LocationConfigurations();

        LocationConfiguration amazon = new LocationConfiguration();
        amazon.setImports(Lists.newArrayList("http://www.getcloudify.org/spec/cloudify/" + CFY_VERSION + "/types.yaml",
                "http://www.getcloudify.org/spec/aws-plugin/" + CFY_AWS_PLUGIN_VERSION + "/plugin.yaml",
                "http://www.getcloudify.org/spec/diamond-plugin/" + CFY_DIAMOND_VERSION + "/plugin.yaml"));
        amazon.setDsl(CFY_DSL_1_3);
        locationConfigurations.setAmazon(amazon);

        LocationConfiguration openstack = new LocationConfiguration();
        openstack.setImports(Lists.newArrayList("http://www.getcloudify.org/spec/cloudify/" + CFY_VERSION + "/types.yaml",
                "http://www.getcloudify.org/spec/openstack-plugin/" + CFY_OPENSTACK_PLUGIN_VERSION + "/plugin.yaml",
                "http://www.getcloudify.org/spec/diamond-plugin/" + CFY_DIAMOND_VERSION + "/plugin.yaml"));
        openstack.setDsl(CFY_DSL_1_3);
        locationConfigurations.setOpenstack(openstack);

        LocationConfiguration byon = new LocationConfiguration();
        byon.setImports(Lists.newArrayList("http://www.getcloudify.org/spec/cloudify/" + CFY_VERSION + "/types.yaml",
                "http://www.getcloudify.org/spec/host-pool-plugin/" + CFY_BYON_PLUGIN_VERSION + "/plugin.yaml",
                "http://www.getcloudify.org/spec/diamond-plugin/" + CFY_DIAMOND_VERSION + "/plugin.yaml"));
        byon.setDsl(CFY_DSL_1_3);
        locationConfigurations.setByon(byon);

        cloudConfiguration.setLocations(locationConfigurations);
        return cloudConfiguration;
    }

    @Override
    public CloudifyOrchestrator newInstance() {
        /**
         * Hierarchy of context (parent on the left) :
         * Alien Context --> Factory Context --> Real Cloudify 3 context
         * Each cloud will create a different cloudify 3 context
         */
        AnnotationConfigApplicationContext pluginContext = new AnnotationConfigApplicationContext();
        pluginContext.setParent(factoryContext);
        pluginContext.setClassLoader(factoryContext.getClassLoader());
        pluginContext.register(PluginContextConfiguration.class);
        pluginContext.refresh();
        log.info("Created new Cloudify 3 context {} for factory {}", pluginContext.getId(), factoryContext.getId());
        CloudifyOrchestrator provider = pluginContext.getBean(CloudifyOrchestrator.class);
        contextMap.put(provider, pluginContext);
        return provider;
    }

    @Override
    public Map<String, PropertyDefinition> getDeploymentPropertyDefinitions() {
        return deploymentPropertiesService.getDeploymentProperties();
    }

    @Override
    public void destroy(CloudifyOrchestrator instance) {
        AnnotationConfigApplicationContext context = contextMap.remove(instance);
        if (context == null) {
            log.warn("Context not found for paaS provider instance {}", instance);
        } else {
            log.info("Dispose context created for paaS provider {}", instance);
            context.close();
        }
    }

    @Override
    public LocationSupport getLocationSupport() {
        // TODO dynamically search in spring context for locations support
        return new LocationSupport(false, new String[] { "openstack", "amazon", "byon" });
    }

    @Override
    public ArtifactSupport getArtifactSupport() {
        return new ArtifactSupport(new String[] { "tosca.artifacts.Implementation.Bash", "alien.artifacts.BatchScript" });
    }

    @Override
    public String getType() {
        return "Cloudify3";
    }

}