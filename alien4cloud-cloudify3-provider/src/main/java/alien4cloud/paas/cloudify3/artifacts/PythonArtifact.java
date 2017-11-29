package alien4cloud.paas.cloudify3.artifacts;

import java.util.Map;

import org.springframework.stereotype.Component;

import alien4cloud.paas.cloudify3.configuration.CloudConfiguration;

/**
 * Unix bash artifact support. Note that it actually uses the same python script wrapper as the windows batch artifact.
 */
@Component
public class PythonArtifact implements ICloudifyImplementationArtifact {
    @Override
    public String getArtifactName() {
        return "tosca.artifacts.Implementation.Python";
    }

    @Override
    public String getVelocityWrapperPath() {
        return "artifacts/pyScripts.vm";
    }

    @Override
    public void updateVelocityWrapperContext(Map<String, Object> wrapperContext, CloudConfiguration cloudConfiguration) {
    }

    @Override
    public boolean hostAgentExecution() {
        return true;
    }
}
