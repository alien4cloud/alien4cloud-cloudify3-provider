package alien4cloud.paas.cloudify3.artifacts;

import java.util.Map;

import alien4cloud.paas.cloudify3.configuration.CloudConfiguration;
import org.springframework.stereotype.Component;

/**
 * Unix bash artifact support. Note that it actually uses the same python script wrapper as the windows batch artifact.
 */
@Component
public class BashArtifact implements ICloudifyImplementationArtifact {
    @Override
    public String getArtifactName() {
        return "tosca.artifacts.Implementation.Bash";
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
