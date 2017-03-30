package alien4cloud.paas.cloudify3.service.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.types.CapabilityType;
import org.alien4cloud.tosca.model.types.NodeType;
import org.alien4cloud.tosca.model.types.RelationshipType;

import alien4cloud.paas.cloudify3.util.mapping.IPropertyMapping;
import alien4cloud.paas.model.PaaSNodeTemplate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    private List<NodeType> nonNativesTypes;

    private List<RelationshipType> nonNativesRelationshipTypes;

    private List<NodeType> nativeTypes;

    /** Nodes that derived from tosca.nodes.Container.Application.DockerContainer */
    private List<PaaSNodeTemplate> dockerTypes;

    private Map<String, PaaSNodeTemplate> allNodes;

    /** Nodes that are custom resources (provided as types but native by nature). */
    private Map<String, PaaSNodeTemplate> customResources;

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

    /** Maps containing all capability types (capability name, capability type)*/
    private Map<String,CapabilityType> capabilityTypes;

    /**
     * * {elementType -> {propertyNamePath -> IPropertyMapping}}
     */
    private Map<String, Map<String, List<IPropertyMapping>>> propertyMappings;


}
