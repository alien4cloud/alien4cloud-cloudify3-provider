package alien4cloud.paas.cloudify3.util;

import java.nio.file.Path;
import java.nio.file.Paths;

import alien4cloud.component.repository.ArtifactRepositoryConstants;
import alien4cloud.model.components.IArtifact;
import alien4cloud.paas.cloudify3.configuration.MappingConfiguration;

/**
 * Manage artifact locations in the archive.
 */
public final class ArtifactUtil {
    private static final String ARTIFACT_DIRECTORY = "artifacts";

    private ArtifactUtil() {
    }

    /**
     * Get the target path of an artifact in the generated archive.
     *
     * @param rootPath The root path in which the archive is generated.
     * @param mappingConfiguration The mapping configuration to get the directory in which the topology artifacts are stored.
     * @param nodeId The id of the node for which to copy the artifact, allows to avoid collision when multiple instances of the same type are defined in the
     *            topology and have different artifacts.
     * @param artifact The artifact overriden from the topology (placed in alien internal repository).
     * @param originalArtifact The artifact defined in the node type.
     * @return The path in which to copy the artifact when generating the cloudify archive.
     */
    public static Path getInternalRepositoryArtifactPath(Path rootPath, MappingConfiguration mappingConfiguration, String nodeId, IArtifact artifact,
            IArtifact originalArtifact) {
        Path targetPath = rootPath.resolve(mappingConfiguration.getTopologyArtifactDirectoryName()).resolve(nodeId).resolve(originalArtifact.getArchiveName());

        if (originalArtifact.getArtifactRef() == null || originalArtifact.getArtifactRef().isEmpty()) {
            targetPath = targetPath.resolve(artifact.getArtifactRef());
        } else {
            targetPath = targetPath.resolve(originalArtifact.getArtifactRef());
        }

        return targetPath;
    }

    /**
     * Get the target path of an artifact in the generated archive.
     *
     * @param mappingConfiguration The mapping configuration to get the directory in which the topology artifacts are stored.
     * @param nodeId The id of the node for which to copy the artifact, allows to avoid collision when multiple instances of the same type are defined in the
     *            topology and have different artifacts.
     * @param artifact The artifact overriden from the topology (placed in alien internal repository).
     * @param originalArtifact The artifact defined in the node type.
     * @return The path as string relative to the archive that defines the artifact location.
     */
    public static String getInternalRepositoryArtifactPath(MappingConfiguration mappingConfiguration, String nodeId, IArtifact artifact,
            IArtifact originalArtifact) {
        StringBuilder sb = new StringBuilder(mappingConfiguration.getTopologyArtifactDirectoryName()).append("/").append(nodeId).append("/")
                .append(originalArtifact.getArchiveName()).append("/");

        if (originalArtifact.getArtifactRef() == null || originalArtifact.getArtifactRef().isEmpty()) {
            sb.append(artifact.getArtifactRef());
        } else {
            sb.append(originalArtifact.getArtifactRef());
        }

        return sb.toString();
    }

    /**
     * Get the path of an artifact from a tosca type in the cloudify generated archive.
     * 
     * @param rootPath The archive root path.
     * @param artifact The artifact for which to get the target Path
     * @return The path of the artifact in the cloudify archive relative to the given root path.
     */
    public static Path getToscaArchiveArtifactPath(Path rootPath, IArtifact artifact) {
        return rootPath.resolve(ARTIFACT_DIRECTORY).resolve(artifact.getArchiveName()).resolve(artifact.getArtifactRef());
    }

    /**
     * Get the path of an artifact from a tosca type in the cloudify generated archive.
     *
     * @param artifact The artifact for which to get the target Path
     * @return The path of the artifact in the cloudify archive.
     */
    public static String getToscaArchiveArtifactPath(IArtifact artifact) {
        return ARTIFACT_DIRECTORY + "/" + artifact.getArchiveName() + "/" + artifact.getArtifactRef();
    }
}
