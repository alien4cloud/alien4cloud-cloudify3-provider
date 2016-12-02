package alien4cloud.paas.cloudify3.location;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import alien4cloud.model.deployment.matching.MatchingConfiguration;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.plugin.ILocationResourceAccessor;
import alien4cloud.paas.cloudify3.CloudifyOrchestratorFactory;
import alien4cloud.paas.cloudify3.configuration.KubernetesLocationConfiguration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KubernetesLocationConfigurator extends AbstractLocationConfigurator {
    @Inject
    private ResourceGenerator resourceGenerator;

    @Override
    protected String[] getLocationArchivePaths() {
        return new String[] { "provider/kubernetes/configuration" };
    }

    @Override
    public List<String> getResourcesTypes() {
        return getAllResourcesTypes();
    }

    @Override
    public List<LocationResourceTemplate> instances(ILocationResourceAccessor resourceAccessor) {
        return Lists.newArrayList();
    }

    @Override
    public Set<String> getManagedLocationTypes() {
        return Sets.newHashSet("kubernetes");
    }

    @Override
    public Map<String, MatchingConfiguration> getMatchingConfigurations() {
        return Maps.newHashMap();
    }

    public static KubernetesLocationConfiguration getDefaultConfiguration() {
        KubernetesLocationConfiguration k8s = new KubernetesLocationConfiguration();
        k8s.setImports(Lists.newArrayList("http://www.getcloudify.org/spec/cloudify/3.4/types.yaml",
                "http://www.getcloudify.org/spec/diamond-plugin/" + CloudifyOrchestratorFactory.CFY_DIAMOND_VERSION + "/plugin.yaml",
                "http://www.getcloudify.org/spec/fabric-plugin/1.4.1/plugin.yaml",
                "plugins/cloudify-kubernetes-plugin/plugin-remote.yaml",
                "plugins/cloudify-proxy-plugin/plugin.yaml"));
        k8s.setDsl("cloudify_dsl_1_3");

        return k8s;
    }
}
