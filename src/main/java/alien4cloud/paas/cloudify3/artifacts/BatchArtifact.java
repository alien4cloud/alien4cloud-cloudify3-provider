package alien4cloud.paas.cloudify3.artifacts;

import java.util.Map;

import org.springframework.stereotype.Component;

import alien4cloud.paas.cloudify3.configuration.CloudConfiguration;

/**
 * Windows batch artifact support. Note that it actually uses the same python script wrapper as the bash artifact.
 */
@Component
public class BatchArtifact implements ICloudifyImplementationArtifact {
    @Override
    public String getArtifactName() {
        return "alien.artifacts.BatchScript";
    }

    @Override
    public String getVelocityWrapperPath() {
        return "artifacts/scripts.vm";
    }

    @Override
    public void updateVelocityWrapperContext(Map<String, Object> wrapperContext, CloudConfiguration cloudConfiguration) {
    }

    @Override
    public boolean hostAgentExecution() {
        return true;
    }
}
