package alien4cloud.paas.cloudify3.artifacts;

import alien4cloud.paas.cloudify3.configuration.CloudConfiguration;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DockerArtifact implements ICloudifyImplementationArtifact {
    @Override
    public String getArtifactName() {
        return "tosca.artifacts.Deployment.Image.Container.Docker";
    }

    @Override
    public String getVelocityWrapperPath() {
        return null;
    }

    @Override
    public void updateVelocityWrapperContext(Map<String, Object> wrapperContext, CloudConfiguration cloudConfiguration) {
    }

    @Override
    public boolean hostAgentExecution() {
        return false;
    }
}
