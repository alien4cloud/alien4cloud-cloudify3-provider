package alien4cloud.paas.cloudify3.artifacts;

import java.util.Map;

import org.springframework.stereotype.Component;

import alien4cloud.paas.cloudify3.configuration.CloudConfiguration;

/**
 * Windows batch artifact support. Note that it actually uses the same python script wrapper as the bash artifact.
 */
public abstract class AbstractBatchArtifact implements ICloudifyImplementationArtifact {

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
