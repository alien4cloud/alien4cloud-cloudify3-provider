package alien4cloud.paas.cloudify3.service.model;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;

import org.alien4cloud.tosca.model.definitions.DeploymentArtifact;
import org.alien4cloud.tosca.model.definitions.IValue;
import org.alien4cloud.tosca.model.definitions.ImplementationArtifact;
import org.alien4cloud.tosca.model.definitions.Operation;
import org.alien4cloud.tosca.model.definitions.OperationOutput;
import alien4cloud.paas.IPaaSTemplate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * Wrapper for a real operation, give extension to deployment artifacts and others
 */
@AllArgsConstructor
public class OperationWrapper extends Operation {

    @Getter
    @Setter
    private IPaaSTemplate<?> owner;

    private Operation delegate;

    @Getter
    private String interfaceName;

    @Getter
    private String operationName;

    /**
     * node id --> artifact_name --> artifact
     */
    @Getter
    @Setter
    private Map<String, Map<String, DeploymentArtifact>> allDeploymentArtifacts;

    /**
     * (id of the relationship, source node id) --> artifact_name --> artifact
     */
    @Getter
    @Setter
    private Map<Relationship, Map<String, DeploymentArtifact>> allRelationshipDeploymentArtifacts;

    @Override
    public ImplementationArtifact getImplementationArtifact() {
        return delegate.getImplementationArtifact();
    }

    @Override
    public Map<String, IValue> getInputParameters() {
        if (delegate.getInputParameters() == null) {
            return Maps.newLinkedHashMap();
        }
        return Maps.newLinkedHashMap(delegate.getInputParameters());
    }

    public Set<OperationOutput> getOutputs() {
        return delegate.getOutputs();
    }

}
