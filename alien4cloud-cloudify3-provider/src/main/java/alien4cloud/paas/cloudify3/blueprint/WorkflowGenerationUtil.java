package alien4cloud.paas.cloudify3.blueprint;

import java.nio.file.Path;

import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.alien4cloud.tosca.model.workflow.activities.CallOperationWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.DelegateWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.SetStateWorkflowActivity;
import org.apache.commons.lang3.StringUtils;

import alien4cloud.paas.cloudify3.configuration.MappingConfiguration;
import alien4cloud.paas.cloudify3.service.PropertyEvaluatorService;
import alien4cloud.paas.cloudify3.service.model.CloudifyDeployment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkflowGenerationUtil extends AbstractGenerationUtil {

    public WorkflowGenerationUtil(MappingConfiguration mappingConfiguration, CloudifyDeployment alienDeployment, Path recipePath,
            PropertyEvaluatorService propertyEvaluatorService) {
        super(mappingConfiguration, alienDeployment, recipePath, propertyEvaluatorService);
    }

    public boolean isSetStateTask(WorkflowStep step) {
        return StringUtils.isEmpty(step.getTargetRelationship()) && step.getActivity() instanceof SetStateWorkflowActivity;
    }

    public boolean isOperationExecutionTask(WorkflowStep step) {
        return StringUtils.isEmpty(step.getTargetRelationship()) && step.getActivity() instanceof CallOperationWorkflowActivity;
    }

    public boolean isRelationshipOperationExecutionTask(WorkflowStep step) {
        return StringUtils.isNotEmpty(step.getTargetRelationship()) && step.getActivity() instanceof CallOperationWorkflowActivity;
    }

    public String getTargetIdOfRelationship(CloudifyDeployment deployment, String target, String targetRelationship) {
        return deployment.getAllNodes().get(target).getRelationshipTemplate(targetRelationship, target).getTemplate().getTarget();
    }

    public WorkflowStep getWorkflowStep(Workflow wf, String stepName) {
        return wf.getSteps().get(stepName);
    }

    public boolean isDelegateActivityStep(WorkflowStep step) {
        return StringUtils.isEmpty(step.getTargetRelationship()) && step.getActivity() instanceof DelegateWorkflowActivity;
    }

    /**
     * Just format the string to make it usable as a method name.
     */
    public String getPythonNormalizedString(String input) {
        return input.replaceAll("-", "_").replaceAll(" ", "_").toLowerCase();
    }

}
