package alien4cloud.paas.cloudify3.artifacts;

/**
 * Interface to implement by Beans that offers artifact support for cfy 3 plugin.
 */
public interface ICloudifyImplementationArtifact {
    /**
     * Return the TOSCA name of the artifact.
     * 
     * @return The TOSCA name of the artifact.
     */
    String getArtifactName();

    /**
     * Return the path of the wrapper's velocity template.
     * 
     * @return The wrapper velocity template.
     */
    String getVelocityWrapperPath();
}