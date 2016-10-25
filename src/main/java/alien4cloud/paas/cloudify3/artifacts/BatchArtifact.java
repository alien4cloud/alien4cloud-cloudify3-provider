package alien4cloud.paas.cloudify3.artifacts;

/**
 * Windows batch artifact support. Note that it actually uses the same python script wrapper as the bash artifact.
 */
public class BatchArtifact implements ICloudifyImplementationArtifact {
    @Override
    public String getArtifactName() {
        return "alien.artifacts.BatchScript";
    }

    @Override
    public String getVelocityWrapperPath() {
        return "velocity/script_wrapper.vm";
    }
}
