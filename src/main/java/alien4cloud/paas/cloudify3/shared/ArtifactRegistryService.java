package alien4cloud.paas.cloudify3.shared;

import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import com.google.common.collect.Maps;

import alien4cloud.paas.cloudify3.artifacts.ICloudifyImplementationArtifact;

/**
 * Registry of supported artifacts.
 */
@Service
public class ArtifactRegistryService {
    @Inject
    private ApplicationContext context;
    private Map<String, ICloudifyImplementationArtifact> artifactMap;

    @PostConstruct
    public void initialize() {
        artifactMap = Maps.newHashMap();
        for (ICloudifyImplementationArtifact cloudifyImplementationArtifact : context.getBeansOfType(ICloudifyImplementationArtifact.class).values()) {
            artifactMap.put(cloudifyImplementationArtifact.getArtifactName(), cloudifyImplementationArtifact);
        }
    }

    public ICloudifyImplementationArtifact getCloudifyImplementationArtifact(String artifactType) {
        return artifactMap.get(artifactType);
    }

    /**
     * Get an array of the supported artifact types.
     * 
     * @return An array of the supported artifact types.
     */
    public String[] getSupportedArtifactTypes() {
        Set<String> artifactTypeSet = artifactMap.keySet();
        return artifactTypeSet.toArray(new String[artifactTypeSet.size()]);
    }
}