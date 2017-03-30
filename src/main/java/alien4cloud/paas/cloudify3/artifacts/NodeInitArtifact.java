package alien4cloud.paas.cloudify3.artifacts;

import java.util.Map;

import org.springframework.stereotype.Component;

import alien4cloud.paas.cloudify3.configuration.CloudConfiguration;

/**
 * Virtual artifact injected to force the create operation on every node to perform TOSCA initializations:
 * <ul>
 * <li>Inject properties as attributes</li>
 * <li>Inject ip_address attribute to endpoints capabilities</li>
 * </ul>
 */
@Component
public class NodeInitArtifact implements ICloudifyImplementationArtifact {
    public static final String DO_NOTHING_IMPL_ARTIFACT_TYPE = "org.alien4cloud.artifacts.cfy.NodeInit";

    @Override
    public String getArtifactName() {
        return DO_NOTHING_IMPL_ARTIFACT_TYPE;
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
