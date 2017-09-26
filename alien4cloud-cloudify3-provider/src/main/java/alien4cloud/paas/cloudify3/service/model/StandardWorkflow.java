package alien4cloud.paas.cloudify3.service.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import alien4cloud.paas.wf.AbstractStep;

@Getter
@Setter
public class StandardWorkflow {

    /**
     * Steps that are not related to any host.
     */
    private Map<String, AbstractStep> orphanSteps;

    /**
     * from Orphan links.
     */
    private List<WorkflowStepLink> links;

    private Set<String> hosts;
}
