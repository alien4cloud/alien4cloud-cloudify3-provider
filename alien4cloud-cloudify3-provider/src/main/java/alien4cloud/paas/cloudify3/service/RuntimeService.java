package alien4cloud.paas.cloudify3.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.inject.Inject;

import alien4cloud.rest.utils.JsonUtil;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;

import alien4cloud.paas.cloudify3.configuration.CfyConnectionManager;
import alien4cloud.paas.cloudify3.error.DeploymentRuntimeException;
import alien4cloud.paas.cloudify3.model.Deployment;
import alien4cloud.paas.cloudify3.model.Execution;
import alien4cloud.paas.cloudify3.model.ExecutionStatus;
import alien4cloud.paas.cloudify3.model.NodeInstance;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class to manage deployment's runtime.
 */
@Slf4j
public abstract class RuntimeService {
    @Resource
    protected ListeningScheduledExecutorService scheduledExecutorService;
    @Inject
    private CfyConnectionManager configurationHolder;

    private ListenableFuture<Execution> internalWaitForExecutionFinish(final ListenableFuture<Execution> futureExecution) {
        AsyncFunction<Execution, Execution> waitFunc = execution -> {
            if (ExecutionStatus.isTerminatedSuccessfully(execution.getStatus()) || ExecutionStatus.isCancelled(execution.getStatus())) {
                log.info("Execution {} for workflow {} has finished with status {}", execution.getId(), execution.getWorkflowId(), execution.getStatus());
                return futureExecution;
            } else if (ExecutionStatus.isTerminatedWithFailure(execution.getStatus())) {
                String errMessage = String.format("Execution %s for workflow %s fails with error:\n %s", execution.getId(), execution.getWorkflowId(), execution.getError());
                log.info(errMessage);
                Map<String, String> errMap = ImmutableMap.of("executionId", execution.getId(), "workflowId", execution.getWorkflowId(), "error", execution.getError());
                return Futures.immediateFailedFuture(new DeploymentRuntimeException(JsonUtil.toString(errMap)));
            } else {
                // If it's not finished, schedule another poll in 2 seconds
                ListenableFuture<Execution> newFutureExecution = Futures.dereference(scheduledExecutorService
                        .schedule(() -> configurationHolder.getApiClient().getExecutionClient().asyncRead(execution.getId()), 2, TimeUnit.SECONDS));
                return internalWaitForExecutionFinish(newFutureExecution);
            }
        };
        return Futures.transform(futureExecution, waitFunc);
    }

    private ListenableFuture<Deployment> internalWaitForDeploymentDeleted(final ListenableFuture<Deployment> futureDeployment) {
        AsyncFunction<Deployment, Deployment> waitFunc = deployment -> {
            if (deployment == null) {
                return Futures.immediateFuture(null);
            } else {
                ListenableFuture<Deployment> newFutureExecution = Futures.dereference(scheduledExecutorService
                        .schedule(() -> configurationHolder.getApiClient().getDeploymentClient().asyncRead(deployment.getId()), 2, TimeUnit.SECONDS));
                return Futures.withFallback(internalWaitForDeploymentDeleted(newFutureExecution), throwable -> Futures.immediateFuture(null));
            }
        };
        return Futures.transform(futureDeployment, waitFunc);
    }

    protected ListenableFuture<Deployment> waitForDeploymentDeleted(String deploymentPaaSId) {
        return internalWaitForDeploymentDeleted(Futures.withFallback(configurationHolder.getApiClient().getDeploymentClient().asyncRead(deploymentPaaSId),
                throwable -> Futures.immediateFuture(null)));
    }

    /**
     * This methods uses Futures.transform to recursively check that the execution provided as a parameter is completed (by calling cloudify rest api) before
     * returning it.
     *
     * @param futureExecution The future execution that has been triggered but may not be completed.
     * @return The future execution that is now completed.
     */
    protected ListenableFuture<Deployment> waitForExecutionFinish(final ListenableFuture<Execution> futureExecution) {
        ListenableFuture<Execution> finishedExecution = internalWaitForExecutionFinish(futureExecution);
        final AsyncFunction<Execution, Deployment> waitSystemWorkflow = execution -> waitForDeploymentExecutionsFinish(
                configurationHolder.getApiClient().getDeploymentClient().asyncRead(execution.getDeploymentId()));
        return Futures.transform(finishedExecution, waitSystemWorkflow);
    }

    /**
     * Return a function that waits recursively until the deployment is created in cloudify (All pending executions to create the deployment are indeed
     * completed).
     *
     * @return An AsyncFunction that will return the Deployment only after all pending executions to create the deployment are completed.
     */
    protected ListenableFuture<Deployment> waitForDeploymentExecutionsFinish(final ListenableFuture<Deployment> futureDeployment) {
        if (log.isDebugEnabled()) {
            log.debug("Begin waiting for all executions finished for deployment");
        }
        AsyncFunction<Deployment, Deployment> waitFunc = deployment -> {
            final ListenableFuture<Execution[]> futureExecutions = waitForDeploymentExecutionsFinish(deployment.getId(),
                    configurationHolder.getApiClient().getExecutionClient().asyncList(deployment.getId(), true));
            Function<Execution[], Deployment> adaptFunc = input -> {
                log.info("All execution has finished for deployment {}", deployment.getId());
                return deployment;
            };
            return Futures.transform(futureExecutions, adaptFunc);
        };
        return Futures.transform(futureDeployment, waitFunc);
    }

    private ListenableFuture<Execution[]> waitForDeploymentExecutionsFinish(final String deploymentId, final ListenableFuture<Execution[]> futureExecutions) {
        AsyncFunction<Execution[], Execution[]> waitFunc = executions -> {
            boolean allExecutionFinished = true;
            if (log.isDebugEnabled()) {
                log.debug("Deployment {} has {} executions", deploymentId, executions.length);
            }
            for (Execution execution : executions) {
                if (!ExecutionStatus.isTerminated(execution.getStatus())) {
                    allExecutionFinished = false;
                    if (log.isDebugEnabled()) {
                        log.debug("Execution {} for deployment {} has not terminated {}", execution.getWorkflowId(), execution.getDeploymentId(),
                                execution.getStatus());
                    }
                    break;
                } else if (log.isDebugEnabled()) {
                    log.debug("Execution {} for deployment {} has terminated {}", execution.getWorkflowId(), execution.getDeploymentId(),
                            execution.getStatus());
                }
            }
            if (allExecutionFinished && executions.length > 0) {
                return futureExecutions;
            } else {
                // If it's not finished, schedule another poll in 2 seconds
                ListenableFuture<Execution[]> newFutureExecutions = Futures.dereference(scheduledExecutorService
                        .schedule(() -> configurationHolder.getApiClient().getExecutionClient().asyncList(deploymentId, true), 2, TimeUnit.SECONDS));
                return waitForDeploymentExecutionsFinish(deploymentId, newFutureExecutions);
            }
        };
        return Futures.transform(futureExecutions, waitFunc);
    }

    protected ListenableFuture<NodeInstance[]> cancelAllRunningExecutions(final String deploymentPaaSId) {
        ListenableFuture<Execution[]> currentExecutions = configurationHolder.getApiClient().getExecutionClient().asyncList(deploymentPaaSId, true);
        AsyncFunction<Execution[], ?> abortCurrentExecutionsFunction = (AsyncFunction<Execution[], List<Object>>) executions -> {
            List<ListenableFuture<?>> abortExecutions = Lists.newArrayList();
            for (Execution execution : executions) {
                if (!ExecutionStatus.isTerminated(execution.getStatus())) {
                    if (!execution.getIsSystemWorkflow()) {
                        log.info("Cancel running user workflow execution " + execution.getWorkflowId());
                        abortExecutions
                                .add(waitForExecutionFinish(configurationHolder.getApiClient().getExecutionClient().asyncCancel(execution.getId(), true)));
                    } else {
                        log.info("Wait for system execution finished " + execution.getWorkflowId());
                        abortExecutions.add(waitForExecutionFinish(Futures.immediateFuture(execution)));
                    }
                }
            }
            return Futures.allAsList(abortExecutions);
        };
        ListenableFuture<?> abortCurrentExecutionsFuture = Futures.transform(currentExecutions, abortCurrentExecutionsFunction);
        AsyncFunction<Object, NodeInstance[]> livingNodesRetrievalFunction = input -> configurationHolder.getApiClient().getNodeInstanceClient()
                .asyncList(deploymentPaaSId);
        return Futures.transform(abortCurrentExecutionsFuture, livingNodesRetrievalFunction);
    }
}
