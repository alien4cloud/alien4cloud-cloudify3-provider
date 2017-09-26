package alien4cloud.paas.cloudify3.artifacts;

import alien4cloud.paas.cloudify3.configuration.CloudConfiguration;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Windows batch artifact support. Note that it actually uses the same python script wrapper as the bash artifact.
 */
@Component
public class BatchArtifact extends AbstractBatchArtifact {

    @Override
    public String getArtifactName() {
        return "org.alien4cloud.artifacts.BatchScript";
    }

}
