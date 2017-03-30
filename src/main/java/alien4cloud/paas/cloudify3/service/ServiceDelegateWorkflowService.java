package alien4cloud.paas.cloudify3.service;

import static alien4cloud.utils.AlienUtils.safe;
import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.INSTALL;

import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Maps;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
import alien4cloud.paas.plan.ToscaNodeLifecycleConstants;
import alien4cloud.paas.wf.AbstractActivity;
import alien4cloud.paas.wf.AbstractStep;
import alien4cloud.paas.wf.DelegateWorkflowActivity;
import alien4cloud.paas.wf.NodeActivityStep;
import alien4cloud.paas.wf.OperationCallActivity;
import alien4cloud.paas.wf.SetStateActivity;
import alien4cloud.paas.wf.Workflow;
import lombok.extern.slf4j.Slf4j;

/**
 * This service is responsible for replacing the delegate operations of Service matched nodes by actual create and start operations.
 */
@Slf4j
@Service
public class ServiceDelegateWorkflowService {

    /**
     * Replace the service delegate workflow by a matching workflow.
     *
     * @param serviceNode The node for which to inject the create operation.
     * @param installWorkflow The install workflow for which to change the delegate operation.
     */
    public void replaceInstallServiceDelegate(PaaSNodeTemplate serviceNode, Workflow installWorkflow) {
        Set<String> stepDependencies = Sets.newHashSet();
        for (PaaSRelationshipTemplate relationshipTemplate : serviceNode.getRelationshipTemplates()) {
            if (serviceNode.getId().equals(relationshipTemplate.getSource())) {
                // the start operation (used to trigger the add source and target) needs dependency on target started state.
                String dependentStepId = getStartedStepId(relationshipTemplate.getTemplate().getTarget(), installWorkflow);
                stepDependencies.add(dependentStepId);
            }
        }
        // Replace the delegate activity by create and start operations.
        for (Entry<String, AbstractStep> stepEntry : installWorkflow.getSteps().entrySet()) {
            if (stepEntry.getValue() instanceof NodeActivityStep && serviceNode.getId().equals(((NodeActivityStep) stepEntry.getValue()).getNodeId())) {
                NodeActivityStep nodeStep = (NodeActivityStep) stepEntry.getValue();
                AbstractActivity activity = nodeStep.getActivity();
                if (activity instanceof DelegateWorkflowActivity && INSTALL.equals(((DelegateWorkflowActivity) activity).getWorkflowName())) {
                    // Inject Creating set state step
                    String creatingStepName = ToscaNodeLifecycleConstants.CREATING + "_" + serviceNode.getId();
                    SetStateActivity setStateActivity = new SetStateActivity();
                    setStateActivity.setNodeId(nodeStep.getNodeId());
                    setStateActivity.setStateName(ToscaNodeLifecycleConstants.CREATING);
                    NodeActivityStep creatingStep = new NodeActivityStep(nodeStep.getNodeId(), nodeStep.getHostId(), setStateActivity);
                    creatingStep.setName(creatingStepName);

                    // Inject Created set state step
                    String createdStepName = ToscaNodeLifecycleConstants.CREATED + "_" + serviceNode.getId();
                    setStateActivity = new SetStateActivity();
                    setStateActivity.setNodeId(nodeStep.getNodeId());
                    setStateActivity.setStateName(ToscaNodeLifecycleConstants.CREATED);
                    NodeActivityStep createdStep = new NodeActivityStep(nodeStep.getNodeId(), nodeStep.getHostId(), setStateActivity);
                    createdStep.setName(createdStepName);

                    // Inject started set state step
                    String startedStepName = ToscaNodeLifecycleConstants.STARTED + "_" + serviceNode.getId();
                    setStateActivity = new SetStateActivity();
                    setStateActivity.setNodeId(nodeStep.getNodeId());
                    setStateActivity.setStateName(ToscaNodeLifecycleConstants.STARTED);
                    NodeActivityStep startedStep = new NodeActivityStep(nodeStep.getNodeId(), nodeStep.getHostId(), setStateActivity);
                    startedStep.setName(startedStepName);

                    // Inject the Create operation in the workflow steps
                    String createStepName = ToscaNodeLifecycleConstants.CREATE + "_" + serviceNode.getId();
                    OperationCallActivity createActivity = new OperationCallActivity(ToscaNodeLifecycleConstants.STANDARD, ToscaNodeLifecycleConstants.CREATE);
                    createActivity.setNodeId(nodeStep.getNodeId());
                    NodeActivityStep createStep = new NodeActivityStep(nodeStep.getNodeId(), nodeStep.getHostId(), createActivity);
                    createStep.setName(createStepName);

                    // Inject the Start operation in the workflow steps
                    String startStepName = ToscaNodeLifecycleConstants.START + "_" + serviceNode.getId();
                    OperationCallActivity startActivity = new OperationCallActivity(ToscaNodeLifecycleConstants.STANDARD, ToscaNodeLifecycleConstants.START);
                    startActivity.setNodeId(nodeStep.getNodeId());
                    NodeActivityStep startStep = new NodeActivityStep(nodeStep.getNodeId(), nodeStep.getHostId(), startActivity);
                    startStep.setName(startStepName);

                    installWorkflow.getSteps().put(creatingStepName, creatingStep);
                    installWorkflow.getSteps().put(createdStepName, createdStep);
                    installWorkflow.getSteps().put(createStepName, createStep);
                    installWorkflow.getSteps().put(startStepName, startStep);
                    installWorkflow.getSteps().put(startedStepName, startedStep);

                    creatingStep.setFollowingSteps(Sets.newHashSet(createStepName));
                    creatingStep.setPrecedingSteps(stepEntry.getValue().getPrecedingSteps());
                    for (String precederId : safe(creatingStep.getPrecedingSteps())) {
                        AbstractStep preceder = installWorkflow.getSteps().get(precederId);
                        preceder.getFollowingSteps().remove(stepEntry.getKey());
                        preceder.getFollowingSteps().add(creatingStepName);
                    }

                    createStep.setPrecedingSteps(Sets.newHashSet(creatingStepName));
                    createStep.setFollowingSteps(Sets.newHashSet(createdStepName));
                    createdStep.setPrecedingSteps(Sets.newHashSet(createStepName));
                    createdStep.setFollowingSteps(Sets.newHashSet(startStepName));

                    stepDependencies.add(createdStepName);
                    startStep.setPrecedingSteps(stepDependencies);
                    for (String precederId : startStep.getPrecedingSteps()) {
                        AbstractStep preceder = installWorkflow.getSteps().get(precederId);
                        if (preceder.getFollowingSteps() == null) {
                            preceder.setFollowingSteps(Sets.newHashSet());
                        }
                        preceder.getFollowingSteps().add(startStepName);
                    }
                    startStep.setFollowingSteps(Sets.newHashSet(startedStepName));

                    startedStep.setPrecedingSteps(Sets.newHashSet(startStepName));
                    startedStep.setFollowingSteps(stepEntry.getValue().getFollowingSteps());
                    for (String followerId : safe(startStep.getFollowingSteps())) {
                        AbstractStep follower = installWorkflow.getSteps().get(followerId);
                        follower.getPrecedingSteps().remove(stepEntry.getKey());
                        follower.getPrecedingSteps().add(startedStepName);
                    }

                    installWorkflow.getSteps().remove(stepEntry.getKey());
                    return;
                }
            }
        }
    }

    private String getStartedStepId(String nodeId, Workflow installWorkflow) {
        for (Entry<String, AbstractStep> stepEntry : installWorkflow.getSteps().entrySet()) {
            if (stepEntry.getValue() instanceof NodeActivityStep) {
                NodeActivityStep nodeActivityStep = ((NodeActivityStep) stepEntry.getValue());
                if (nodeId.equals(nodeActivityStep.getNodeId()) && nodeActivityStep.getActivity() instanceof SetStateActivity) {
                    SetStateActivity setStateActivity = (SetStateActivity) nodeActivityStep.getActivity();
                    if (ToscaNodeLifecycleConstants.CREATED.equals(setStateActivity.getStateName())) {
                        return stepEntry.getKey();
                    }
                }
            }
        }
        return null;
    }
}