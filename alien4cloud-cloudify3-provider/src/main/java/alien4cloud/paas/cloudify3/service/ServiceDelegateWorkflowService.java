package alien4cloud.paas.cloudify3.service;

import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.CREATE;
import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.CREATED;
import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.CREATING;
import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.DELETED;
import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.DELETING;
import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.STANDARD;
import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.START;
import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.STARTED;
import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.STARTING;
import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.STOP;
import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.STOPPED;
import static alien4cloud.paas.plan.ToscaNodeLifecycleConstants.STOPPING;
import static alien4cloud.utils.AlienUtils.safe;
import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.INSTALL;
import static org.alien4cloud.tosca.normative.constants.NormativeWorkflowNameConstants.UNINSTALL;

import java.util.Map.Entry;
import java.util.Set;

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
import alien4cloud.paas.wf.util.WorkflowUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * This service is responsible for replacing the delegate operations of Service matched nodes by actual create and start operations.
 */
@Slf4j
@Service
public class ServiceDelegateWorkflowService {
    /**
     * Replace the service delegate workflow by a matching workflow:
     *
     * creating -> create() -> created -> start() -> started
     * start operation requires all dependencies (based on relationship the service is source of) before being executed.
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
                    NodeActivityStep creatingStep = WorkflowUtils.addStateStep(installWorkflow, serviceNode.getId(), CREATING);
                    NodeActivityStep createStep = WorkflowUtils.addOperationStep(installWorkflow, serviceNode.getId(), STANDARD, CREATE);
                    NodeActivityStep createdStep = WorkflowUtils.addStateStep(installWorkflow, serviceNode.getId(), CREATED);

                    NodeActivityStep startingStep = WorkflowUtils.addStateStep(installWorkflow, serviceNode.getId(), STARTING);
                    NodeActivityStep startStep = WorkflowUtils.addOperationStep(installWorkflow, serviceNode.getId(), STANDARD, START);
                    NodeActivityStep startedStep = WorkflowUtils.addStateStep(installWorkflow, serviceNode.getId(), STARTED);

                    // Re-wire the workflow
                    creatingStep.setPrecedingSteps(stepEntry.getValue().getPrecedingSteps());
                    for (String precederId : safe(creatingStep.getPrecedingSteps())) {
                        AbstractStep preceder = installWorkflow.getSteps().get(precederId);
                        preceder.getFollowingSteps().remove(stepEntry.getKey());
                        preceder.getFollowingSteps().add(creatingStep.getName());
                    }
                    creatingStep.setFollowingSteps(Sets.newHashSet(createStep.getName()));

                    createStep.setPrecedingSteps(Sets.newHashSet(creatingStep.getName()));
                    createStep.setFollowingSteps(Sets.newHashSet(createdStep.getName()));

                    createdStep.setPrecedingSteps(Sets.newHashSet(createStep.getName()));
                    createdStep.setFollowingSteps(Sets.newHashSet(startingStep.getName()));

                    startingStep.setPrecedingSteps(stepDependencies);
                    // Inject dependency follower (when node is source)
                    for (String precederId : startingStep.getPrecedingSteps()) {
                        AbstractStep preceder = installWorkflow.getSteps().get(precederId);
                        if (preceder.getFollowingSteps() == null) {
                            preceder.setFollowingSteps(Sets.newHashSet());
                        }
                        preceder.getFollowingSteps().add(startingStep.getName());
                    }
                    startingStep.getPrecedingSteps().add(createdStep.getName());
                    startingStep.setFollowingSteps(Sets.newHashSet(startStep.getName()));

                    startStep.setPrecedingSteps(Sets.newHashSet(startingStep.getName()));
                    startStep.setFollowingSteps(Sets.newHashSet(startedStep.getName()));

                    startedStep.setPrecedingSteps(Sets.newHashSet(startStep.getName()));
                    startedStep.setFollowingSteps(stepEntry.getValue().getFollowingSteps());
                    for (String followerId : safe(startedStep.getFollowingSteps())) {
                        AbstractStep follower = installWorkflow.getSteps().get(followerId);
                        follower.getPrecedingSteps().remove(stepEntry.getKey());
                        follower.getPrecedingSteps().add(startedStep.getName());
                    }

                    // Remove old step
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
                    if (CREATED.equals(setStateActivity.getStateName())) {
                        return stepEntry.getKey();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Replace the delegate operation of the service in the uninstall workflow by a simple:
     * stopping -> stop() -> stopped -> deleting -> deleted
     *
     * @param serviceNode The service node for which to override the delegate operation.
     * @param uninstallWorkflow The uninstall workflow in which to perform the override.
     */
    public void replaceUnInstallServiceDelegate(PaaSNodeTemplate serviceNode, Workflow uninstallWorkflow) {
        // Replace the delegate with the stopping, stop, deleted sequence
        for (Entry<String, AbstractStep> stepEntry : uninstallWorkflow.getSteps().entrySet()) {
            if (stepEntry.getValue() instanceof NodeActivityStep && serviceNode.getId().equals(((NodeActivityStep) stepEntry.getValue()).getNodeId())) {
                NodeActivityStep nodeStep = (NodeActivityStep) stepEntry.getValue();
                AbstractActivity activity = nodeStep.getActivity();
                if (activity instanceof DelegateWorkflowActivity && UNINSTALL.equals(((DelegateWorkflowActivity) activity).getWorkflowName())) {
                    NodeActivityStep stoppingStep = WorkflowUtils.addStateStep(uninstallWorkflow, serviceNode.getId(), STOPPING);
                    NodeActivityStep stopStep = WorkflowUtils.addOperationStep(uninstallWorkflow, serviceNode.getId(), STANDARD, STOP);
                    NodeActivityStep stoppedStep = WorkflowUtils.addStateStep(uninstallWorkflow, serviceNode.getId(), STOPPED);
                    NodeActivityStep deletingStep = WorkflowUtils.addStateStep(uninstallWorkflow, serviceNode.getId(), DELETING);
                    NodeActivityStep deletedStep = WorkflowUtils.addStateStep(uninstallWorkflow, serviceNode.getId(), DELETED);

                    // Re-wire the workflow
                    stoppingStep.setPrecedingSteps(stepEntry.getValue().getPrecedingSteps());
                    for (String precederId : safe(stoppingStep.getPrecedingSteps())) {
                        AbstractStep preceder = uninstallWorkflow.getSteps().get(precederId);
                        preceder.getFollowingSteps().remove(stepEntry.getKey());
                        preceder.getFollowingSteps().add(stoppingStep.getName());
                    }
                    stoppingStep.setFollowingSteps(Sets.newHashSet(stopStep.getName()));

                    stopStep.setPrecedingSteps(Sets.newHashSet(stoppingStep.getName()));
                    stopStep.setFollowingSteps(Sets.newHashSet(stoppedStep.getName()));

                    stoppedStep.setPrecedingSteps(Sets.newHashSet(stopStep.getName()));
                    stoppedStep.setFollowingSteps(Sets.newHashSet(deletingStep.getName()));

                    deletingStep.setPrecedingSteps(Sets.newHashSet(stoppedStep.getName()));
                    deletingStep.setFollowingSteps(Sets.newHashSet(deletedStep.getName()));

                    deletedStep.setPrecedingSteps(Sets.newHashSet(deletingStep.getName()));
                    deletedStep.setFollowingSteps(stepEntry.getValue().getFollowingSteps());
                    for (String followerId : safe(deletedStep.getFollowingSteps())) {
                        AbstractStep follower = uninstallWorkflow.getSteps().get(followerId);
                        follower.getPrecedingSteps().remove(stepEntry.getKey());
                        follower.getPrecedingSteps().add(deletedStep.getName());
                    }

                    // Remove old step
                    uninstallWorkflow.getSteps().remove(stepEntry.getKey());
                    return;
                }
            }
        }
    }
}