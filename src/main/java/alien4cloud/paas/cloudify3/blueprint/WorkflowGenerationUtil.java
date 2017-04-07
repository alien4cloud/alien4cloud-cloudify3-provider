package alien4cloud.paas.cloudify3.blueprint;

import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.INSTALL;
import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.UNINSTALL;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;

import com.google.common.collect.Lists;

import alien4cloud.paas.cloudify3.configuration.MappingConfiguration;
import alien4cloud.paas.cloudify3.service.PropertyEvaluatorService;
import alien4cloud.paas.cloudify3.service.model.CloudifyDeployment;
import alien4cloud.paas.cloudify3.service.model.HostWorkflow;
import alien4cloud.paas.cloudify3.service.model.WorkflowStepLink;
import alien4cloud.paas.cloudify3.service.model.Workflows;
import alien4cloud.paas.wf.AbstractStep;
import alien4cloud.paas.wf.DelegateWorkflowActivity;
import alien4cloud.paas.wf.NodeActivityStep;
import alien4cloud.paas.wf.OperationCallActivity;
import alien4cloud.paas.wf.SetStateActivity;
import alien4cloud.paas.wf.Workflow;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkflowGenerationUtil extends AbstractGenerationUtil {

    public WorkflowGenerationUtil(MappingConfiguration mappingConfiguration, CloudifyDeployment alienDeployment, Path recipePath,
            PropertyEvaluatorService propertyEvaluatorService) {
        super(mappingConfiguration, alienDeployment, recipePath, propertyEvaluatorService);
    }

    public boolean isSetStateTask(AbstractStep step) {
        return step instanceof NodeActivityStep && ((NodeActivityStep) step).getActivity() instanceof SetStateActivity;
    }

    public boolean isOperationExecutionTask(AbstractStep step) {
        return step instanceof NodeActivityStep && ((NodeActivityStep) step).getActivity() instanceof OperationCallActivity;
    }

    public AbstractStep getWorkflowStep(Workflow wf, String stepName) {
        return wf.getSteps().get(stepName);
    }

    public boolean isDelegateActivityStep(AbstractStep step) {
        return step instanceof NodeActivityStep && ((NodeActivityStep) step).getActivity() instanceof DelegateWorkflowActivity;
    }

    public List<WorkflowStepLink> getExternalLinkns(Workflows workflows, String workflowName) {
        switch (workflowName) {
        case INSTALL:
            return getExternalLinks(workflows.getInstallHostWorkflows());
        case UNINSTALL:
            return getExternalLinks(workflows.getUninstallHostWorkflows());
        default:
            return null;
        }
    }

    private List<WorkflowStepLink> getExternalLinks(Map<String, HostWorkflow> installWorkflowSteps) {
        List<WorkflowStepLink> links = Lists.newArrayList();
        if (MapUtils.isNotEmpty(installWorkflowSteps)) {
            for (HostWorkflow wf : installWorkflowSteps.values()) {
                links.addAll(wf.getExternalLinks());
            }
        }
        return links;
    }

    /**
     * Just format the string to make it usable as a method name.
     */
    public String getPythonNormalizedString(String input) {
        return input.replaceAll("-", "_").replaceAll(" ", "_").toLowerCase();
    }

}
