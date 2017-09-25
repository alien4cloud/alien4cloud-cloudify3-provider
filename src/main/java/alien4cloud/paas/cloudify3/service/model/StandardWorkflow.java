package alien4cloud.paas.cloudify3.service.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alien4cloud.tosca.model.workflow.WorkflowStep;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StandardWorkflow {

    /**
     * Steps that are not related to any host.
     */
    private Map<String, WorkflowStep> orphanSteps;

    /**
     * from Orphan links.
     */
    private List<WorkflowStepLink> links;

    private Set<String> hosts;
}
