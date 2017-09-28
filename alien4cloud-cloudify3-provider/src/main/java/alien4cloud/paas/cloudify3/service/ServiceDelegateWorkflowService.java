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

import org.alien4cloud.tosca.model.workflow.NodeWorkflowStep;
import org.alien4cloud.tosca.model.workflow.Workflow;
import org.alien4cloud.tosca.model.workflow.WorkflowStep;
import org.alien4cloud.tosca.model.workflow.activities.AbstractWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.DelegateWorkflowActivity;
import org.alien4cloud.tosca.model.workflow.activities.SetStateWorkflowActivity;
import org.springframework.stereotype.Service;

import com.google.common.collect.Sets;

import alien4cloud.paas.model.PaaSNodeTemplate;
import alien4cloud.paas.model.PaaSRelationshipTemplate;
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
        for (Entry<String, WorkflowStep> stepEntry : installWorkflow.getSteps().entrySet()) {
            if (WorkflowUtils.isNodeStep(stepEntry.getValue(), serviceNode.getId())) {
                WorkflowStep nodeStep = stepEntry.getValue();
                AbstractWorkflowActivity activity = nodeStep.getActivity();
                if (activity instanceof DelegateWorkflowActivity && INSTALL.equals(((DelegateWorkflowActivity) activity).getDelegate())) {
                    // Inject Creating set state step
                    WorkflowStep creatingStep = WorkflowUtils.addStateStep(installWorkflow, serviceNode.getId(), CREATING);
                    WorkflowStep createStep = WorkflowUtils.addOperationStep(installWorkflow, serviceNode.getId(), STANDARD, CREATE);
                    WorkflowStep createdStep = WorkflowUtils.addStateStep(installWorkflow, serviceNode.getId(), CREATED);

                    WorkflowStep startingStep = WorkflowUtils.addStateStep(installWorkflow, serviceNode.getId(), STARTING);
                    WorkflowStep startStep = WorkflowUtils.addOperationStep(installWorkflow, serviceNode.getId(), STANDARD, START);
                    WorkflowStep startedStep = WorkflowUtils.addStateStep(installWorkflow, serviceNode.getId(), STARTED);

                    // Re-wire the workflow
                    creatingStep.setPrecedingSteps(stepEntry.getValue().getPrecedingSteps());
                    for (String precederId : safe(creatingStep.getPrecedingSteps())) {
                        WorkflowStep preceder = installWorkflow.getSteps().get(precederId);
                        preceder.getOnSuccess().remove(stepEntry.getKey());
                        preceder.getOnSuccess().add(creatingStep.getName());
                    }
                    creatingStep.setOnSuccess(Sets.newHashSet(createStep.getName()));

                    createStep.setPrecedingSteps(Sets.newHashSet(creatingStep.getName()));
                    createStep.setOnSuccess(Sets.newHashSet(createdStep.getName()));

                    createdStep.setPrecedingSteps(Sets.newHashSet(createStep.getName()));
                    createdStep.setOnSuccess(Sets.newHashSet(startingStep.getName()));

                    startingStep.setPrecedingSteps(stepDependencies);
                    // Inject dependency follower (when node is source)
                    for (String precederId : startingStep.getPrecedingSteps()) {
                        WorkflowStep preceder = installWorkflow.getSteps().get(precederId);
                        if (preceder.getOnSuccess() == null) {
                            preceder.setOnSuccess(Sets.newHashSet());
                        }
                        preceder.getOnSuccess().add(startingStep.getName());
                    }
                    startingStep.getPrecedingSteps().add(createdStep.getName());
                    startingStep.setOnSuccess(Sets.newHashSet(startStep.getName()));

                    startStep.setPrecedingSteps(Sets.newHashSet(startingStep.getName()));
                    startStep.setOnSuccess(Sets.newHashSet(startedStep.getName()));

                    startedStep.setPrecedingSteps(Sets.newHashSet(startStep.getName()));
                    startedStep.setOnSuccess(stepEntry.getValue().getOnSuccess());
                    for (String followerId : safe(startedStep.getOnSuccess())) {
                        WorkflowStep follower = installWorkflow.getSteps().get(followerId);
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
        for (Entry<String, WorkflowStep> stepEntry : installWorkflow.getSteps().entrySet()) {
            if (stepEntry.getValue() instanceof NodeWorkflowStep) {
                WorkflowStep WorkflowStep = stepEntry.getValue();
                if (nodeId.equals(WorkflowStep.getTarget()) && WorkflowStep.getActivity() instanceof SetStateWorkflowActivity) {
                    SetStateWorkflowActivity setStateActivity = (SetStateWorkflowActivity) WorkflowStep.getActivity();
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
        for (Entry<String, WorkflowStep> stepEntry : uninstallWorkflow.getSteps().entrySet()) {
            if (WorkflowUtils.isNodeStep(stepEntry.getValue(), serviceNode.getId())) {
                WorkflowStep nodeStep = (WorkflowStep) stepEntry.getValue();
                AbstractWorkflowActivity activity = nodeStep.getActivity();
                if (activity instanceof DelegateWorkflowActivity && UNINSTALL.equals(((DelegateWorkflowActivity) activity).getDelegate())) {
                    WorkflowStep stoppingStep = WorkflowUtils.addStateStep(uninstallWorkflow, serviceNode.getId(), STOPPING);
                    WorkflowStep stopStep = WorkflowUtils.addOperationStep(uninstallWorkflow, serviceNode.getId(), STANDARD, STOP);
                    WorkflowStep stoppedStep = WorkflowUtils.addStateStep(uninstallWorkflow, serviceNode.getId(), STOPPED);
                    WorkflowStep deletingStep = WorkflowUtils.addStateStep(uninstallWorkflow, serviceNode.getId(), DELETING);
                    WorkflowStep deletedStep = WorkflowUtils.addStateStep(uninstallWorkflow, serviceNode.getId(), DELETED);

                    // Re-wire the workflow
                    stoppingStep.setPrecedingSteps(stepEntry.getValue().getPrecedingSteps());
                    for (String precederId : safe(stoppingStep.getPrecedingSteps())) {
                        WorkflowStep preceder = uninstallWorkflow.getSteps().get(precederId);
                        preceder.getOnSuccess().remove(stepEntry.getKey());
                        preceder.getOnSuccess().add(stoppingStep.getName());
                    }
                    stoppingStep.setOnSuccess(Sets.newHashSet(stopStep.getName()));

                    stopStep.setPrecedingSteps(Sets.newHashSet(stoppingStep.getName()));
                    stopStep.setOnSuccess(Sets.newHashSet(stoppedStep.getName()));

                    stoppedStep.setPrecedingSteps(Sets.newHashSet(stopStep.getName()));
                    stoppedStep.setOnSuccess(Sets.newHashSet(deletingStep.getName()));

                    deletingStep.setPrecedingSteps(Sets.newHashSet(stoppedStep.getName()));
                    deletingStep.setOnSuccess(Sets.newHashSet(deletedStep.getName()));

                    deletedStep.setPrecedingSteps(Sets.newHashSet(deletingStep.getName()));
                    deletedStep.setOnSuccess(stepEntry.getValue().getOnSuccess());
                    for (String followerId : safe(deletedStep.getOnSuccess())) {
                        WorkflowStep follower = uninstallWorkflow.getSteps().get(followerId);
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