package alien4cloud.paas.cloudify3.artifacts;

import java.util.Map;

import alien4cloud.paas.cloudify3.configuration.CloudConfiguration;

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

    /**
     * Callback that allows the artifact implementation to add variables to the velocity context.
     *
     * @param wrapperContext The velocity context for the implementation artifact wrapper.
     * @param cloudConfiguration The orchestrator configuration.
     */
    void updateVelocityWrapperContext(Map<String, Object> wrapperContext, CloudConfiguration cloudConfiguration);

    /**
     * If true the artifact will never be executed on the host machine but on the manager host.
     *
     * @return True if the artifact is to be executed on the manager host, false if the artifact support host execution.
     */
    boolean hostAgentExecution();
}