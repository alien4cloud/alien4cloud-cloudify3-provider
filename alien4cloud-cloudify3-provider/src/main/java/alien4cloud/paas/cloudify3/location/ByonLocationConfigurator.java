package alien4cloud.paas.cloudify3.location;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import alien4cloud.model.deployment.matching.MatchingConfiguration;
import alien4cloud.model.orchestrators.locations.LocationResourceTemplate;
import alien4cloud.orchestrators.plugin.ILocationResourceAccessor;

@Component
public class ByonLocationConfigurator extends AbstractLocationConfigurator {
    @Override
    protected String[] getLocationArchivePaths() {
        return new String[] { "provider/byon/configuration" };
    }

    @Override
    public List<String> getResourcesTypes() {
        return getAllResourcesTypes();
    }

    @Override
    public List<LocationResourceTemplate> instances(ILocationResourceAccessor resourceAccessor) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Auto-config is not supported for this location.");
    }

    @Override
    public Set<String> getManagedLocationTypes() {
        return Sets.newHashSet("byon");
    }

    @Override
    public Map<String, MatchingConfiguration> getMatchingConfigurations() {
        return getMatchingConfigurations("provider/openstack/matching/config.yml");
    }
}
