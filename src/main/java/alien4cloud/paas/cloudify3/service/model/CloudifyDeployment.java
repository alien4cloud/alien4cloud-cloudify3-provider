package alien4cloud.paas.cloudify3.service.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import alien4cloud.model.components.DeploymentArtifact;
import alien4cloud.model.components.IndexedNodeType;
import alien4cloud.model.components.IndexedRelationshipType;
import alien4cloud.paas.cloudify3.util.mapping.IPropertyMapping;
import alien4cloud.paas.model.PaaSNodeTemplate;

@Getter
@Setter
@SuppressWarnings("PMD.UnusedPrivateField")
@AllArgsConstructor
@NoArgsConstructor
public class CloudifyDeployment {

    /**
     * This id is a human readable paaS id that will be used to identify deployment on cloudify 3
     */
    private String deploymentPaaSId;

    /**
     * This id is technical alien deployment id that will be used to generate events and send back to alien
     */
    private String deploymentId;

    /**
     * The type of the location retrieved from alien's deployment topology
     */
    private String locationType;

    private List<PaaSNodeTemplate> computes;

    private List<PaaSNodeTemplate> volumes;

    private List<PaaSNodeTemplate> externalNetworks;

    private List<PaaSNodeTemplate> internalNetworks;

    private List<PaaSNodeTemplate> nonNatives;

    private List<IndexedNodeType> nonNativesTypes;

    private List<IndexedRelationshipType> nonNativesRelationshipTypes;

    private List<IndexedNodeType> nativeTypes;

    private Map<String, PaaSNodeTemplate> allNodes;

    private Map<String, List<PaaSNodeTemplate>> groups;

    /**
     * node id --> artifact_name --> artifact
     */
    private Map<String, Map<String, DeploymentArtifact>> allDeploymentArtifacts;

    /**
     * (id of the relationship, source node id) --> artifact_name --> artifact
     */
    private Map<Relationship, Map<String, DeploymentArtifact>> allRelationshipDeploymentArtifacts;

    private Map<String, String> providerDeploymentProperties;

    private Workflows workflows;

    private Set<PaaSNodeTemplate> nodesToMonitor;

    /**
     * * {elementType -> {propertyNamePath -> IPropertyMapping}}
     */
    private Map<String, Map<String, List<IPropertyMapping>>> propertyMappings;
}
