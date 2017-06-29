package alien4cloud.paas.cloudify3.location;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import alien4cloud.model.deployment.matching.MatchingConfiguration;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.plugin.ILocationResourceAccessor;

import com.google.common.collect.Sets;

@Slf4j
@Component
public class OpenstackLocationConfigurator extends AbstractLocationConfigurator {
    @Inject
    private ResourceGenerator resourceGenerator;

    public static final String COMPUTE_TYPE = "alien.nodes.openstack.Compute";
    public static final String WINDOWS_COMPUTE_TYPE = "alien.nodes.openstack.WindowsCompute";
    public static final String IMAGE_TYPE = "alien.nodes.openstack.Image";
    public static final String WINDOWS_IMAGE_TYPE = "alien.nodes.openstack.WindowsImage";
    public static final String FLAVOR_TYPE = "alien.nodes.openstack.Flavor";
    private static final String IMAGE_ID_PROP = "image";
    private static final String FLAVOR_ID_PROP = "flavor";

    @Override
    protected String[] getLocationArchivePaths() {
        return new String[] { "provider/openstack/configuration" };
    }

    @Override
    public List<String> getResourcesTypes() {
        return getAllResourcesTypes();
    }

    @Override
    public List<LocationResourceTemplate> instances(ILocationResourceAccessor resourceAccessor) {
        List<LocationResourceTemplate> instances = resourceGenerator.generateComputes(COMPUTE_TYPE, IMAGE_TYPE, FLAVOR_TYPE, IMAGE_ID_PROP, FLAVOR_ID_PROP,
                resourceAccessor);
        instances.addAll(
                resourceGenerator.generateComputes(WINDOWS_COMPUTE_TYPE, WINDOWS_IMAGE_TYPE, FLAVOR_TYPE, IMAGE_ID_PROP, FLAVOR_ID_PROP, resourceAccessor));
        return instances;
    }

    @Override
    public Set<String> getManagedLocationTypes() {
        return Sets.newHashSet("openstack");
    }

    @Override
    public Map<String, MatchingConfiguration> getMatchingConfigurations() {
        return getMatchingConfigurations("provider/openstack/matching/config.yml");
    }
}
