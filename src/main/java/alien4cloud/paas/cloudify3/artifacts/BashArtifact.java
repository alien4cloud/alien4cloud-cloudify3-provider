package alien4cloud.paas.cloudify3.artifacts;

/**
 * Unix bash artifact support. Note that it actually uses the same python script wrapper as the windows batch artifact.
 */
public class BashArtifact implements ICloudifyImplementationArtifact {
    @Override
    public String getArtifactName() {
        return "tosca.artifacts.Implementation.Bash";
    }

    @Override
    public String getVelocityWrapperPath() {
        return "velocity/script_wrapper.vm";
    }
}
