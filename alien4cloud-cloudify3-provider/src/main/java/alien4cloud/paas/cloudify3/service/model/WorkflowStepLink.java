package alien4cloud.paas.cloudify3.service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A link between 2 steps in the workflow.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStepLink {

    private String fromStepId;

    private String toStepId;

}
