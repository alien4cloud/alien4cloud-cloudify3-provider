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
public class AmazonLocationConfigurator extends AbstractLocationConfigurator {
    @Inject
    private ResourceGenerator resourceGenerator;

    public static final String COMPUTE_TYPE = "alien.cloudify.aws.nodes.Compute";
    public static final String WINDOWS_COMPUTE_TYPE = "alien.cloudify.aws.nodes.WindowsCompute";
    public static final String IMAGE_TYPE = "alien.cloudify.aws.nodes.Image";
    public static final String FLAVOR_TYPE = "alien.cloudify.aws.nodes.InstanceType";
    private static final String IMAGE_ID_PROP = "image_id";
    private static final String FLAVOR_ID_PROP = "instance_type";

    @Override
    protected String[] getLocationArchivePaths() {
        return new String[] { "provider/amazon/configuration" };
    }

    @Override
    public List<String> getResourcesTypes() {
        return getAllResourcesTypes();
    }

    @Override
    public List<LocationResourceTemplate> instances(ILocationResourceAccessor resourceAccessor) {
        return resourceGenerator.generateComputes(COMPUTE_TYPE, WINDOWS_COMPUTE_TYPE, IMAGE_TYPE, FLAVOR_TYPE, IMAGE_ID_PROP, FLAVOR_ID_PROP, resourceAccessor);
    }

    @Override
    public Set<String> getManagedLocationTypes() {
        return Sets.newHashSet("amazon");
    }

    @Override
    public Map<String, MatchingConfiguration> getMatchingConfigurations() {
        return getMatchingConfigurations("provider/amazon/matching/config.yml");
    }
}
