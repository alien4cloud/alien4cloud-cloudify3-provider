package alien4cloud.paas.cloudify3.service;

import static alien4cloud.utils.AlienUtils.safe;

import java.util.Map.Entry;

import org.alien4cloud.tosca.model.definitions.ImplementationArtifact;
import org.alien4cloud.tosca.model.definitions.Interface;
import org.alien4cloud.tosca.model.definitions.Operation;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.alien4cloud.tosca.model.workflow.activities.AbstractWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.CallOperationWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.SetStateWorkflowActivity;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import alien4cloud.paas.cloudify3.artifacts.NodeInitArtifact;
import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.paas.wf.util.WorkflowUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Inject a node init operation to the node and workflow to actually perform some attribute values initializations.
 */
@Slf4j
@Service
public class InitOperationInjectorService {
    // An artifact type that does not do anything but still force the cloudify to actually call something (for create post-initialization).
    public static final String INIT_INTERFACE_TYPE = "org.alien4cloud.interfaces.cfy.lifecycle";
    public static final String INIT_OPERATION_NAME = "NodeInit";

    private static final Interface INIT_INTERFACE;

    static {
        ImplementationArtifact doNothing = new ImplementationArtifact();
        doNothing.setArtifactType(NodeInitArtifact.NODE_INIT_ARTIFACT_TYPE);
        INIT_INTERFACE = new Interface(INIT_INTERFACE_TYPE);
        Operation initOperation = new Operation(doNothing);
        INIT_INTERFACE.getOperations().put(INIT_OPERATION_NAME, initOperation);
    }

    /**
     * Ensure that the node has a create operation and inject a do nothing create operation for pure initialization if not.
     * 
     * @param node The node for which to inject the create operation.
     */
    public void ensureCreateOperation(PaaSNodeTemplate node, Workflow installWorkflow) {
        // Add the init interface to the node.
        node.getInterfaces().put(INIT_INTERFACE_TYPE, INIT_INTERFACE);
        // Inject the call to the init interface into the workflow just after the node created state change.
        for (Entry<String, WorkflowStep> stepEntry : installWorkflow.getSteps().entrySet()) {
            if (WorkflowUtils.isNodeStep(stepEntry.getValue(), node.getId())) {
                AbstractWorkflowActivity activity = stepEntry.getValue().getActivity();
                if (isCreatingStep(node, activity)) {
                    String stepName = "_a4c_init_" + node.getId();
                    // Inject the NodeInit operation in the workflow steps
                    CallOperationWorkflowActivity callActivity = new CallOperationWorkflowActivity(INIT_INTERFACE_TYPE, INIT_OPERATION_NAME);
                    callActivity.setTarget(stepEntry.getValue().getTarget());
                    WorkflowStep initStep = new WorkflowStep(stepEntry.getValue().getTarget(), stepEntry.getValue().getHostId(), callActivity);
                    initStep.setName(stepName);
                    initStep.setOnSuccess(stepEntry.getValue().getOnSuccess());
                    for (String followerId : safe(initStep.getOnSuccess())) {
                        WorkflowStep follower = installWorkflow.getSteps().get(followerId);
                        follower.getPrecedingSteps().remove(stepEntry.getKey());
                        follower.getPrecedingSteps().add(stepName);
                    }
                    initStep.setPrecedingSteps(Sets.newHashSet(stepEntry.getKey()));
                    installWorkflow.getSteps().put(stepName, initStep);
                    stepEntry.getValue().setOnSuccess(Sets.newHashSet(stepName));
                    return;
                }
            }
        }
        log.error("Unable to find creating step and to inject init operation for node {}.", node.getId());
    }

    private boolean isCreatingStep(PaaSNodeTemplate node, AbstractWorkflowActivity activity) {
        return activity instanceof SetStateWorkflowActivity
                && ToscaNodeLifecycleConstants.CREATING.equals(((SetStateWorkflowActivity) activity).getStateName());
    }
}