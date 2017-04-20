package alien4cloud.paas.cloudify3.artifacts;

import org.springframework.stereotype.Component;

/**
 * Windows batch artifact support. Note that it actually uses the same python script wrapper as the bash artifact.
 */
@Component
@Deprecated
public class DeprecatedBatchArtifact extends AbstractBatchArtifact {

    @Override
    public String getArtifactName() {
        return "alien.artifacts.BatchScript";
    }

}
